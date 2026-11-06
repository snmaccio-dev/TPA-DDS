package donatrack.web.sesion;

public record ConfiguracionOidc(
    String urlKeycloak,
    String realm,
    String clientId,
    String urlBaseWeb
) {

  public ConfiguracionOidc {
    if (urlKeycloak == null || urlKeycloak.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL de Keycloak.");
    }
    if (realm == null || realm.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el realm de Keycloak.");
    }
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el client id del front.");
    }
    if (urlBaseWeb == null || urlBaseWeb.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del cliente web.");
    }

    urlKeycloak = sinBarraFinal(urlKeycloak);
    urlBaseWeb = sinBarraFinal(urlBaseWeb);
  }

  public String urlDeAutorizacion() {
    return raizDelRealm() + "/protocol/openid-connect/auth";
  }

  public String urlDeToken() {
    return raizDelRealm() + "/protocol/openid-connect/token";
  }

  public String urlDeCierreDeSesion() {
    return raizDelRealm() + "/protocol/openid-connect/logout";
  }

  public String urlDeRetorno() {
    return urlBaseWeb + "/callback";
  }

  private String raizDelRealm() {
    return urlKeycloak + "/realms/" + realm;
  }

  private static String sinBarraFinal(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
