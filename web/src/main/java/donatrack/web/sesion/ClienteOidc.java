package donatrack.web.sesion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import donatrack.web.integracion.ServicioNoDisponibleException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class ClienteOidc {

  private static final String SERVICIO = "identidad";
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
    return urlDeIngreso(estado, desafio, false, null);
  }

  // pedirCredenciales agrega prompt=login: Keycloak muestra el formulario aunque haya una
  // sesion SSO abierta (por ejemplo, de otra cuenta). loginHint (opcional) agrega
  // login_hint para completar el usuario del formulario.
  public String urlDeIngreso(String estado,
                             String desafio,
                             boolean pedirCredenciales,
                             String loginHint) {
    StringBuilder url = new StringBuilder(configuracion.urlDeAutorizacion())
        .append("?response_type=code")
        .append("&client_id=").append(codificar(configuracion.clientId()))
        .append("&redirect_uri=").append(codificar(configuracion.urlDeRetorno()))
        .append("&scope=").append(codificar("openid email profile"))
        .append("&state=").append(codificar(estado))
        .append("&code_challenge=").append(codificar(desafio))
        .append("&code_challenge_method=S256");
    if (pedirCredenciales) {
      url.append("&prompt=login");
    }
    if (loginHint != null && !loginHint.isBlank()) {
      url.append("&login_hint=").append(codificar(loginHint.trim()));
    }
    return url.toString();
  }

  public String urlDeSalida(String idToken) {
    String url = configuracion.urlDeCierreDeSesion()
        + "?post_logout_redirect_uri=" + codificar(configuracion.urlBaseWeb())
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

  public TokensDeKeycloak renovar(String refreshToken) {
    return pedirTokens("grant_type=refresh_token"
        + "&client_id=" + codificar(configuracion.clientId())
        + "&refresh_token=" + codificar(refreshToken));
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
      throw new ServicioNoDisponibleException(SERVICIO, "respondió " + estado + " al pedir el token");
    }

    JsonNode nodo = interpretar(respuesta.body());
    String accessToken = nodo.path("access_token").asText(null);

    if (accessToken == null || accessToken.isBlank()) {
      throw new ServicioNoDisponibleException(SERVICIO, "respondió sin access_token");
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
      throw new ServicioNoDisponibleException(
          SERVICIO, "devolvió una respuesta que no se pudo interpretar", e
      );
    }
  }

  private HttpResponse<String> enviar(HttpRequest pedido) {
    try {
      return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServicioNoDisponibleException(SERVICIO, "no respondió a tiempo", e);
    } catch (Exception e) {
      throw new ServicioNoDisponibleException(SERVICIO, "no está disponible", e);
    }
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }
}
