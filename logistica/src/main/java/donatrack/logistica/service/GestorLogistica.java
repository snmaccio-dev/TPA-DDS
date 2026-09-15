package donatrack.logistica.service;

import donatrack.logistica.contrato.DestinoPlanificado;
import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoRutasPlanificadas;
import donatrack.logistica.contrato.PlanificacionRecibida;
import donatrack.logistica.contrato.RutaPlanificada;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.planificacion.GeneradorRutas;
import donatrack.logistica.domain.planificacion.ResultadoPlanificacion;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioCamiones;
import donatrack.logistica.repository.RepositorioEntregas;
import donatrack.logistica.repository.RepositorioRutas;

import java.util.ArrayList;
import java.util.List;

public class GestorLogistica {

  private static final int TAMANIO_MAXIMO_DE_LOTE = 100;
  private static final int INTENTOS_ANTES_DE_ESCALAR = 3;

  private final GeneradorRutas generadorRutas;
  private final ClienteDonaciones clienteDonaciones;
  private final String urlCallback;

  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();
  private final RepositorioEntregas repositorioEntregas =
      RepositorioEntregas.getInstance();
  private final RepositorioCamiones repositorioCamiones =
      RepositorioCamiones.getInstance();

  public GestorLogistica(GeneradorRutas generadorRutas,
                         ClienteDonaciones clienteDonaciones,
                         String urlCallback) {
    if (generadorRutas == null) {
      throw new IllegalArgumentException("Debe indicarse el generador de rutas.");
    }
    if (clienteDonaciones == null) {
      throw new IllegalArgumentException("Debe indicarse el cliente del servicio de Donaciones.");
    }
    if (urlCallback == null || urlCallback.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL de callback del planificador.");
    }
    this.generadorRutas = generadorRutas;
    this.clienteDonaciones = clienteDonaciones;
    this.urlCallback = urlCallback;
  }

  // Toma las donaciones ya asignadas en Donaciones y las incorpora como entregas pendientes
  public List<Entrega> incorporarDonacionesAsignadas() {
    List<Entrega> nuevas = clienteDonaciones.donacionesConAsignacionRealizada().stream()
        .filter(this::todaviaNoTieneEntrega)
        .map(Entrega::desde)
        .toList();

    repositorioEntregas.guardar(nuevas);
    return nuevas;
  }

  // Envia al proveedor externo las entregas pendientes, en lotes del tamanio maximo admitido
  public List<String> planificarRutas(List<Camion> camiones) {
    if (camiones == null || camiones.isEmpty()) {
      throw new IllegalArgumentException("No hay camiones disponibles para planificar rutas.");
    }

    List<String> solicitudes = new ArrayList<>();

    for (List<Entrega> lote : dividirEnLotes(repositorioEntregas.queEsperanPlanificacion())) {
      lote.forEach(Entrega::registrarIntentoDePlanificacion);
      solicitudes.add(
          generadorRutas.solicitarPlanificacion(lote, camiones, urlCallback)
      );
    }

    return solicitudes;
  }

  public void procesarResultadoPlanificacion(ResultadoPlanificacion resultado) {
    if (resultado == null) {
      throw new IllegalArgumentException("El planificador no devolvio un resultado.");
    }

    List<RutaReparto> rutas = resultado.getRutas();
    repositorioRutas.guardar(rutas);

    rutas.forEach(ruta ->
        clienteDonaciones.publicarRutasPlanificadas(
            new EventoRutasPlanificadas(ruta.getId(), donacionIdsDe(ruta))
        )
    );
  }

  // Una corrida completa: trae lo asignado en Donaciones y lo manda a planificar
  public List<String> ejecutarCorrida() {
    incorporarDonacionesAsignadas();
    return planificarRutas(repositorioCamiones.todas());
  }

  public List<Entrega> entregasPendientes() {
    return repositorioEntregas.queEsperanPlanificacion();
  }

  // Traduce lo que devuelve el proveedor externo a objetos de dominio antes de procesarlo
  public ResultadoPlanificacion procesarCallbackDelProveedor(PlanificacionRecibida recibida) {
    if (recibida == null) {
      throw new IllegalArgumentException("El planificador no devolvio un resultado.");
    }

    List<RutaReparto> rutas = recibida.rutas().stream()
        .map(this::armarRuta)
        .toList();

    List<Entrega> noAsignadas = recibida.entregaIdsNoAsignadas().stream()
        .map(this::buscarEntrega)
        .toList();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(recibida.solicitudId(), rutas, noAsignadas);

    procesarResultadoPlanificacion(resultado);

    return resultado;
  }

  public List<Entrega> entregasQueSuperaronLosIntentos() {
    return repositorioEntregas.queEsperanPlanificacion().stream()
        .filter(entrega -> entrega.getIntentosDePlanificacion() >= INTENTOS_ANTES_DE_ESCALAR)
        .toList();
  }

  private RutaReparto armarRuta(RutaPlanificada planificada) {
    Camion camion = repositorioCamiones.buscar(planificada.patenteCamion())
        .orElseThrow(() ->
            new IllegalArgumentException(
                "El planificador asigno un camion inexistente: " + planificada.patenteCamion()
            ));

    List<DestinoEntrega> destinos = planificada.destinos().stream()
        .map(this::armarDestino)
        .toList();

    return new RutaReparto(camion, destinos);
  }

  private DestinoEntrega armarDestino(DestinoPlanificado planificado) {
    List<Entrega> entregas = planificado.entregaIds().stream()
        .map(this::buscarEntrega)
        .toList();

    return new DestinoEntrega(planificado.direccion(), entregas);
  }

  private Entrega buscarEntrega(long entregaId) {
    return repositorioEntregas.buscarPorId(entregaId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "El planificador referencio una entrega inexistente: " + entregaId
            ));
  }

  private boolean todaviaNoTieneEntrega(DonacionParaPlanificar donacion) {
    return repositorioEntregas.buscarPorDonacion(donacion.donacionId()).isEmpty();
  }

  private List<Long> donacionIdsDe(RutaReparto ruta) {
    return ruta.getEntregas().stream()
        .map(Entrega::getDonacionId)
        .toList();
  }

  private List<List<Entrega>> dividirEnLotes(List<Entrega> entregas) {
    List<List<Entrega>> lotes = new ArrayList<>();

    for (int desde = 0; desde < entregas.size(); desde += TAMANIO_MAXIMO_DE_LOTE) {
      int hasta = Math.min(desde + TAMANIO_MAXIMO_DE_LOTE, entregas.size());
      lotes.add(new ArrayList<>(entregas.subList(desde, hasta)));
    }

    return lotes;
  }
}
