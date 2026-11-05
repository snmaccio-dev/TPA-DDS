package donatrack.logistica.infrastructure.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import donatrack.logistica.domain.integracion.IntegracionExternaException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

// Pide tokens a Keycloak con client_credentials (service account del client
// "logistica-backend", cuyo rol asignado es SERVICIO_LOGISTICA) y los
// cachea hasta un ratito antes del vencimiento, para no pedir un token nuevo
// en cada llamada a Donaciones.
public class ProveedorDeTokenDeServicio {

  private static final Duration ESPERA = Duration.ofSeconds(10);
  private static final Duration MARGEN_RENOVACION = Duration.ofSeconds(30);

  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();
  private final String urlBase;
  private final String realm;
  private final String clientId;
  private final String clientSecret;

  private volatile String tokenEnCache;
  private volatile Instant vencimientoToken = Instant.EPOCH;

  public ProveedorDeTokenDeServicio(String urlBase, String realm,
                                    String clientId, String clientSecret) {
    this(urlBase, realm, clientId, clientSecret,
        HttpClient.newBuilder().connectTimeout(ESPERA).build());
  }

  // Constructor para tests: HttpClient inyectado.
  public ProveedorDeTokenDeServicio(String urlBase, String realm,
                                    String clientId, String clientSecret,
                                    HttpClient http) {
    if (urlBase == null || urlBase.isBlank()) {
      throw new IllegalArgumentException("La URL de Keycloak es obligatoria.");
    }
    if (realm == null || realm.isBlank()) {
      throw new IllegalArgumentException("El realm es obligatorio.");
    }
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("El client id es obligatorio.");
    }
    if (clientSecret == null || clientSecret.isBlank()) {
      throw new IllegalArgumentException("El client secret es obligatorio.");
    }
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.realm = realm;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.http = http;
  }

  public String obtener() {
    String cacheado = this.tokenEnCache;
    Instant vence = this.vencimientoToken;
    if (cacheado != null && Instant.now().isBefore(vence)) {
      return cacheado;
    }
    return refrescar();
  }

  private synchronized String refrescar() {
    if (this.tokenEnCache != null && Instant.now().isBefore(this.vencimientoToken)) {
      return this.tokenEnCache;
    }

    String cuerpo = "grant_type=client_credentials"
        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

    HttpResponse<String> respuesta;
    try {
      respuesta = http.send(
          HttpRequest.newBuilder()
              .uri(URI.create(urlBase + "/realms/" + realm + "/protocol/openid-connect/token"))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .timeout(ESPERA)
              .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
              .build(),
          HttpResponse.BodyHandlers.ofString()
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IntegracionExternaException("Se interrumpio el pedido de token a Keycloak.", e);
    } catch (Exception e) {
      throw new IntegracionExternaException("No se pudo pedir el token a Keycloak.", e);
    }

    if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
      throw new IntegracionExternaException(
          "Keycloak respondio " + respuesta.statusCode() + " al pedir el token de servicio."
      );
    }
    JsonNode nodo;
    try {
      nodo = json.readTree(respuesta.body());
    } catch (Exception e) {
      throw new IntegracionExternaException("Keycloak devolvio un JSON invalido para el token.", e);
    }

    String accessToken = nodo.path("access_token").asText(null);
    long expiresIn = nodo.path("expires_in").asLong(0);
    if (accessToken == null || accessToken.isBlank()) {
      throw new IntegracionExternaException("Keycloak respondio sin access_token.");
    }
    this.tokenEnCache = accessToken;
    this.vencimientoToken = Instant.now().plusSeconds(Math.max(1, expiresIn))
        .minus(MARGEN_RENOVACION);
    return accessToken;
  }
}