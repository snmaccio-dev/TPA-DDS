package donatrack.logistica.seguridad.web;

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
import java.util.Objects;

public class ClienteOidc {

  private static final String SERVICIO = "El servicio de identidad";
  private static final Duration ESPERA = Duration.ofSeconds(30);

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(ESPERA)
      .build();

  private final ObjectMapper json = new ObjectMapper();
  private final ConfiguracionOidc configuracion;

  public ClienteOidc(ConfiguracionOidc configuracion) {
    Objects.requireNonNull(configuracion, "Debe indicarse la configuracion de OIDC.");
    this.configuracion = configuracion;
  }

  public String urlDeIngreso(String estado, String desafio) {
    return configuracion.urlDeAutorizacion()
        + "?response_type=code"
        + "&client_id=" + codificar(configuracion.clientId())
        + "&redirect_uri=" + codificar(configuracion.urlDeRetorno())
        + "&scope=" + codificar("openid email profile")
        + "&state=" + codificar(estado)
        + "&code_challenge=" + codificar(desafio)
        + "&code_challenge_method=S256";
  }

  public String urlDeSalida(String idToken) {
    String url = configuracion.urlDeCierreDeSesion()
        + "?post_logout_redirect_uri=" + codificar(configuracion.urlWeb())
        + "&client_id=" + codificar(configuracion.clientId());

    return idToken == null || idToken.isBlank()
        ? url
        : url + "&id_token_hint=" + codificar(idToken);
  }

  public TokensDeKeycloak intercambiar(String codigo, String verificador) {
    return pedirTokens("grant_type=authorization_code"
        + "&client_id=" + codificar(configuracion.clientId())
        + "&redirect_uri=" + codificar(configuracion.urlDeRetorno())
        + "&code=" + codificar(codigo)
        + "&code_verifier=" + codificar(verificador));
  }

  private TokensDeKeycloak pedirTokens(String cuerpo) {
    HttpResponse<String> respuesta = enviar(
        HttpRequest.newBuilder()
            .uri(URI.create(configuracion.urlDeToken()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(ESPERA)
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build()
    );

    int estado = respuesta.statusCode();

    if (estado < 200 || estado >= 300) {
      throw new IntegracionExternaException(
          SERVICIO + " respondió " + estado + " al pedir el token.");
    }

    JsonNode nodo = interpretar(respuesta.body());
    String accessToken = nodo.path("access_token").asText(null);

    if (accessToken == null || accessToken.isBlank()) {
      throw new IntegracionExternaException(SERVICIO + " respondió sin access_token.");
    }

    return new TokensDeKeycloak(
        accessToken,
        nodo.path("refresh_token").asText(null),
        nodo.path("id_token").asText(null),
        nodo.path("expires_in").asLong(0)
    );
  }

  private JsonNode interpretar(String cuerpo) {
    try {
      return json.readTree(cuerpo);
    } catch (Exception e) {
      throw new IntegracionExternaException(
          SERVICIO + " devolvió una respuesta que no se pudo interpretar.", e
      );
    }
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IntegracionExternaException(SERVICIO + " no respondió a tiempo.", e);
    } catch (Exception e) {
      throw new IntegracionExternaException(SERVICIO + " no está disponible.", e);
    }
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }
}
