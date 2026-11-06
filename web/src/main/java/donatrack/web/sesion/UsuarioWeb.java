package donatrack.web.sesion;

import donatrack.web.vista.PerfilVista;

public record UsuarioWeb(
    String accessToken,
    String idToken,
    PerfilVista perfil
) {

  public static UsuarioWeb deDemostracion() {
    return new UsuarioWeb(null, null, PerfilVista.deDemostracion());
  }

  public String autorizacion() {
    return accessToken == null || accessToken.isBlank() ? null : "Bearer " + accessToken;
  }
}
