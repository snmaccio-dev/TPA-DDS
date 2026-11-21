package donatrack.logistica.infrastructure.planificacion;

import donatrack.logistica.contrato.DestinoPlanificado;
import donatrack.logistica.contrato.PlanificacionRecibida;
import donatrack.logistica.contrato.RutaPlanificada;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.integracion.SerializadorJson;
import donatrack.logistica.domain.planificacion.GeneradorRutas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// Reemplazo local del planificador externo (LOGISTICA_PLANIFICADOR=simulado). Se comporta
// como el proveedor: recibe la solicitud, responde enseguida y, unos segundos despues y en
// otro hilo, manda el plan a /planificacion/callback con el mismo contrato
// (PlanificacionRecibida) y el mismo token (X-Planificador-Token) que usaria el externo.
//
// El plan es simple: solo usa los camiones disponibles (sin ruta planificada ni en curso),
// carga cada entrega en el primer camion donde entra por peso y volumen, arma un destino
// por direccion y deja como no asignadas las entregas que no entran en ninguno.
public class GeneradorRutasSimulado implements GeneradorRutas {

  public static final Duration DEMORA_POR_DEFECTO = Duration.ofSeconds(3);
  static final String HEADER_TOKEN = "X-Planificador-Token";

  private final String token;
  private final SerializadorJson json;
  private final Supplier<Set<String>> patentesOcupadas;
  private final Duration demora;
  private final ScheduledExecutorService ejecutor;
  private final EnvioDeCallback envio;

  // Lo que hace falta para llamar al callback. En la app es un POST HTTP; los tests lo
  // reemplazan para ver que se mando.
  @FunctionalInterface
  interface EnvioDeCallback {
    int enviar(String url, String token, String cuerpo) throws Exception;
  }

  public GeneradorRutasSimulado(String token,
                                SerializadorJson json,
                                Supplier<Set<String>> patentesOcupadas) {
    this(token, json, patentesOcupadas, DEMORA_POR_DEFECTO,
        Executors.newSingleThreadScheduledExecutor(tarea -> {
          Thread hilo = new Thread(tarea, "planificador-simulado");
          hilo.setDaemon(true);
          return hilo;
        }),
        new EnvioHttp());
  }

  GeneradorRutasSimulado(String token,
                         SerializadorJson json,
                         Supplier<Set<String>> patentesOcupadas,
                         Duration demora,
                         ScheduledExecutorService ejecutor,
                         EnvioDeCallback envio) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el token del planificador.");
    }
    this.token = token;
    this.json = Objects.requireNonNull(json, "Debe indicarse el serializador JSON.");
    this.patentesOcupadas =
        Objects.requireNonNull(patentesOcupadas, "Debe indicarse como saber que camiones estan ocupados.");
    this.demora = demora;
    this.ejecutor = ejecutor;
    this.envio = envio;
  }

  @Override
  public void solicitarPlanificacion(
      String solicitudId,
      List<Entrega> entregas,
      List<Camion> camiones,
      String urlCallback) {

    // El plan se arma ahora, mientras las entregas y los camiones siguen cargados en la
    // transaccion de la corrida; lo que viaja al otro hilo son solo datos.
    Set<String> ocupados = patentesOcupadas.get();
    List<Camion> disponibles = camiones.stream()
        .filter(camion -> !ocupados.contains(camion.getPatente()))
        .toList();
    PlanificacionRecibida plan = armarPlan(solicitudId, entregas, disponibles);

    System.out.println("[PLANIFICADOR SIMULADO] solicitud " + solicitudId
        + " | entregas: " + entregas.size()
        + " | camiones disponibles: " + disponibles.stream().map(Camion::getPatente).toList()
        + " | rutas: " + plan.rutas().size()
        + " | no asignadas: " + plan.entregaIdsNoAsignadas().size()
        + " | responde en " + demora.toSeconds() + " s a " + urlCallback);

    String cuerpo = json.aJson(plan);
    ejecutor.schedule(
        () -> responder(solicitudId, urlCallback, cuerpo),
        demora.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  private void responder(String solicitudId, String urlCallback, String cuerpo) {
    try {
      int estado = envio.enviar(urlCallback, token, cuerpo);
      if (estado >= 200 && estado < 300) {
        System.out.println("[PLANIFICADOR SIMULADO] solicitud " + solicitudId
            + " respondida (" + estado + ").");
      } else {
        System.err.println("[PLANIFICADOR SIMULADO] el callback de la solicitud " + solicitudId
            + " respondio " + estado + ".");
      }
    } catch (Exception e) {
      System.err.println("[PLANIFICADOR SIMULADO] no se pudo responder la solicitud "
          + solicitudId + ": " + e.getMessage());
    }
  }

  // Primer camion donde entra (por peso y volumen, en el orden en que vienen los camiones)
  // y un destino por direccion, en el orden de las entregas. Lo que no entra en ningun
  // camion, o no tiene medidas, queda como no asignado.
  static PlanificacionRecibida armarPlan(String solicitudId,
                                         List<Entrega> entregas,
                                         List<Camion> camiones) {
    List<Carga> cargas = camiones.stream().map(Carga::new).toList();
    List<Long> noAsignadas = new ArrayList<>();

    for (Entrega entrega : entregas) {
      Carga elegida = entrega.getPesoKg() == null || entrega.getVolumenM3() == null
          ? null
          : cargas.stream()
              .filter(carga -> carga.entra(entrega))
              .findFirst()
              .orElse(null);
      if (elegida == null) {
        noAsignadas.add(entrega.getId());
      } else {
        elegida.cargar(entrega);
      }
    }

    List<RutaPlanificada> rutas = cargas.stream()
        .filter(Carga::tieneEntregas)
        .map(Carga::comoRuta)
        .toList();
    return new PlanificacionRecibida(solicitudId, rutas, noAsignadas);
  }

  private static final class Carga {

    private final Camion camion;
    private double peso;
    private double volumen;
    private final Map<String, List<Long>> porDireccion = new LinkedHashMap<>();

    Carga(Camion camion) {
      this.camion = camion;
    }

    boolean entra(Entrega entrega) {
      return peso + entrega.getPesoKg() <= camion.getCapacidadCarga()
          && volumen + entrega.getVolumenM3() <= camion.getCapacidadVolumen();
    }

    void cargar(Entrega entrega) {
      peso += entrega.getPesoKg();
      volumen += entrega.getVolumenM3();
      porDireccion.computeIfAbsent(entrega.getDireccionDestino(), d -> new ArrayList<>())
          .add(entrega.getId());
    }

    boolean tieneEntregas() {
      return !porDireccion.isEmpty();
    }

    RutaPlanificada comoRuta() {
      return new RutaPlanificada(camion.getPatente(), porDireccion.entrySet().stream()
          .map(destino -> new DestinoPlanificado(destino.getKey(), destino.getValue()))
          .toList());
    }
  }

  private static final class EnvioHttp implements EnvioDeCallback {

    private static final Duration ESPERA = Duration.ofSeconds(30);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();

    @Override
    public int enviar(String url, String token, String cuerpo) throws Exception {
      HttpRequest pedido = HttpRequest.newBuilder(URI.create(url))
          .timeout(ESPERA)
          .header("Content-Type", "application/json")
          .header(HEADER_TOKEN, token)
          .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
          .build();
      return http.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
  }
}
