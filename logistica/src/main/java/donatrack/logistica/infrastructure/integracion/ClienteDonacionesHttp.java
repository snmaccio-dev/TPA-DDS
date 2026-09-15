package donatrack.logistica.infrastructure.integracion;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.EventoRutasPlanificadas;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class ClienteDonacionesHttp implements ClienteDonaciones {

  private static final String HEADER_EVENTO = "X-Evento-Id";
  private static final int CONFLICTO = 409;
  private static final Duration ESPERA = Duration.ofSeconds(10);

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(ESPERA)
      .build();

  private final String urlBase;
  private final SerializadorJson json;

  public ClienteDonacionesHttp(String urlBase, SerializadorJson json) {
    if (urlBase == null || urlBase.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del servicio de Donaciones.");
    }
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.json = json;
  }

  @Override
  public List<DonacionParaPlanificar> donacionesConAsignacionRealizada() {
    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + "/donaciones?estado=ASIGNACION_REALIZADA"))
            .header("Accept", "application/json")
            .timeout(ESPERA)
            .GET()
            .build()
    );

    if (!fueExitosa(respuesta.statusCode())) {
      throw new IntegracionExternaException(
          "Donaciones respondio " + respuesta.statusCode()
              + " al pedir las donaciones asignadas."
      );
    }

    return json.listaDesdeJson(respuesta.body(), DonacionParaPlanificar.class);
  }

  @Override
  public void publicarRutasPlanificadas(EventoRutasPlanificadas evento) {
    publicar("/donaciones/eventos/rutas-planificadas", evento);
  }

  @Override
  public void publicarInicioRuta(EventoInicioRuta evento) {
    publicar("/donaciones/eventos/inicio-ruta", evento);
  }

  @Override
  public void publicarEntrega(EventoEntrega evento) {
    publicar("/donaciones/eventos/entrega", evento);
  }

  @Override
  public void publicarRetornoADeposito(EventoRetornoDeposito evento) {
    publicar("/donaciones/eventos/retorno-deposito", evento);
  }

  private void publicar(String recurso, Object evento) {
    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + recurso))
            .header("Content-Type", "application/json")
            .header(HEADER_EVENTO, UUID.randomUUID().toString())
            .timeout(ESPERA)
            .POST(HttpRequest.BodyPublishers.ofString(json.aJson(evento)))
            .build()
    );

    int estado = respuesta.statusCode();

    if (estado == CONFLICTO) {
      System.out.println("[DONACIONES] " + recurso + " ya estaba aplicado (409). Se ignora.");
      return;
    }

    if (!fueExitosa(estado)) {
      throw new IntegracionExternaException(
          "Donaciones respondio " + estado + " al publicar el evento en " + recurso + "."
      );
    }
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IntegracionExternaException("Se interrumpio la llamada al servicio de Donaciones.", e);
    } catch (Exception e) {
      throw new IntegracionExternaException("No se pudo contactar al servicio de Donaciones.", e);
    }
  }

  private boolean fueExitosa(int estado) {
    return estado >= 200 && estado < 300;
  }
}
