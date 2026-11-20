package donatrack.logistica.seguridad.web;

// Keycloak se ve desde dos lugares distintos: el navegador lo alcanza por la URL publica
// (http://localhost:8180) y Logistica, dentro de Docker, por la interna
// (http://keycloak:8080). La autorizacion y el logout son redirecciones del navegador;
// el canje del codigo lo hace el servidor.
public record ConfiguracionOidc(
    String urlKeycloak,
    String urlKeycloakPublica,
    String realm,
    String clientId,
    String urlBase,
    String urlWeb
) {

  public ConfiguracionOidc {
    if (urlKeycloak == null || urlKeycloak.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL de Keycloak.");
    }
    if (urlKeycloakPublica == null || urlKeycloakPublica.isBlank()) {
      urlKeycloakPublica = urlKeycloak;
    }
    if (realm == null || realm.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el realm de Keycloak.");
    }
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el client id de la pantalla web.");
    }
    if (urlBase == null || urlBase.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base de Logistica.");
    }
    if (urlWeb == null || urlWeb.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del cliente web.");
    }

    urlKeycloak = sinBarraFinal(urlKeycloak);
    urlKeycloakPublica = sinBarraFinal(urlKeycloakPublica);
    urlBase = sinBarraFinal(urlBase);
    urlWeb = sinBarraFinal(urlWeb);
  }

  public String urlDeAutorizacion() {
    return urlKeycloakPublica + "/realms/" + realm + "/protocol/openid-connect/auth";
  }

  public String urlDeToken() {
    return urlKeycloak + "/realms/" + realm + "/protocol/openid-connect/token";
  }

  public String urlDeCierreDeSesion() {
    return urlKeycloakPublica + "/realms/" + realm + "/protocol/openid-connect/logout";
  }

  public String urlDeRetorno() {
    return urlBase + "/admin/camiones/callback";
  }

  private static String sinBarraFinal(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
