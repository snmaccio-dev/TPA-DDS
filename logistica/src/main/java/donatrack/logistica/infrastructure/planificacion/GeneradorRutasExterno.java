package donatrack.logistica.infrastructure.planificacion;

import donatrack.logistica.contrato.CamionDisponible;
import donatrack.logistica.contrato.EntregaParaPlanificar;
import donatrack.logistica.contrato.SolicitudPlanificacion;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;
import donatrack.logistica.domain.planificacion.GeneradorRutas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class GeneradorRutasExterno implements GeneradorRutas {

  private static final Duration ESPERA = Duration.ofSeconds(30);

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(ESPERA)
      .build();

  private final String urlProveedor;
  private final SerializadorJson json;

  public GeneradorRutasExterno(String urlProveedor, SerializadorJson json) {
    if (urlProveedor == null || urlProveedor.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL del planificador externo.");
    }
    this.urlProveedor = urlProveedor;
    this.json = json;
  }

  @Override
  public String solicitarPlanificacion(
      List<Entrega> entregas,
      List<Camion> camiones,
      String urlCallback) {

    String solicitudId = UUID.randomUUID().toString();

    SolicitudPlanificacion solicitud = new SolicitudPlanificacion(
        solicitudId,
        urlCallback,
        entregas.stream().map(this::comoEntregaDelProveedor).toList(),
        camiones.stream().map(this::comoCamionDelProveedor).toList()
    );

    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlProveedor))
            .header("Content-Type", "application/json")
            .timeout(ESPERA)
            .POST(HttpRequest.BodyPublishers.ofString(json.aJson(solicitud)))
            .build()
    );

    int estado = respuesta.statusCode();

    if (estado < 200 || estado >= 300) {
      throw new IntegracionExternaException(
          "El planificador externo respondio " + estado
              + " a la solicitud " + solicitudId + "."
      );
    }

    return solicitudId;
  }

  private EntregaParaPlanificar comoEntregaDelProveedor(Entrega entrega) {
    return new EntregaParaPlanificar(
        entrega.getId(),
        entrega.getDireccionDestino(),
        entrega.getDescripcionDonacion(),
        entrega.getPesoKg(),
        entrega.getVolumenM3()
    );
  }

  private CamionDisponible comoCamionDelProveedor(Camion camion) {
    return new CamionDisponible(
        camion.getPatente(),
        camion.getCapacidadVolumen(),
        camion.getAltura(),
        camion.getCapacidadCarga()
    );
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IntegracionExternaException("Se interrumpio la llamada al planificador externo.", e);
    } catch (Exception e) {
      throw new IntegracionExternaException("No se pudo contactar al planificador externo.", e);
    }
  }
}
