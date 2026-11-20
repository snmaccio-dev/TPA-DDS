package donatrack.logistica.seguridad.web;

public record TokensDeKeycloak(
    String accessToken,
    String refreshToken,
    String idToken,
    long expiresIn
) {

  // Clave con la que la pantalla de administracion de camiones guarda los tokens en la
  // sesion HTTP. La comparten el login (SesionCamionesController) y FiltroDeAcceso.
  public static final String CLAVE_SESION = "tokensDeKeycloak";
}
