package donatrack.logistica.infrastructure.integracion;

import donatrack.logistica.contrato.PerfilDeUsuario;
import donatrack.logistica.domain.integracion.DirectorioDeUsuarios;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public class DirectorioDeUsuariosHttp implements DirectorioDeUsuarios {

  private static final Duration ESPERA = Duration.ofSeconds(10);
  private static final int NO_AUTORIZADO = 401;
  private static final int PROHIBIDO = 403;

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(ESPERA)
      .build();

  private final String urlBase;
  private final SerializadorJson json;

  public DirectorioDeUsuariosHttp(String urlBase, SerializadorJson json) {
    if (urlBase == null || urlBase.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del servicio de Donaciones.");
    }
    Objects.requireNonNull(json, "Debe indicarse el serializador JSON.");

    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.json = json;
  }

  @Override
  public PerfilDeUsuario perfilDe(String headerAutorizacion) {
    if (headerAutorizacion == null || headerAutorizacion.isBlank()) {
      throw new UnauthorizedResponse("Falta el header Authorization con prefijo Bearer.");
    }

    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(urlBase + "/usuarios/yo"))
            .header("Accept", "application/json")
            .header("Authorization", headerAutorizacion)
            .timeout(ESPERA)
            .GET()
            .build()
    );

    int estado = respuesta.statusCode();

    if (estado == NO_AUTORIZADO) {
      throw new UnauthorizedResponse("Donaciones rechazo el token del usuario.");
    }

    if (estado == PROHIBIDO) {
      throw new ForbiddenResponse("La cuenta no tiene un perfil en Donaciones.");
    }

    if (estado < 200 || estado >= 300) {
      throw new IntegracionExternaException(
          "Donaciones respondio " + estado + " al pedir el perfil del usuario."
      );
    }

    return json.desdeJson(respuesta.body(), PerfilDeUsuario.class);
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IntegracionExternaException(
          "Se interrumpio la consulta del perfil a Donaciones.", e
      );
    } catch (Exception e) {
      throw new IntegracionExternaException(
          "No se pudo contactar a Donaciones para consultar el perfil.", e
      );
    }
  }
}
