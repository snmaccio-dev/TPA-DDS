package donatrack.web.sesion;

import donatrack.web.vista.PerfilVista;

import java.time.Duration;
import java.time.Instant;

public record UsuarioWeb(
    String accessToken,
    String refreshToken,
    String idToken,
    Instant vencimiento,
    PerfilVista perfil
) {

  private static final Duration MARGEN = Duration.ofSeconds(60);

  public static UsuarioWeb deDemostracion() {
    return new UsuarioWeb(null, null, null, null, PerfilVista.deDemostracion());
  }

  public static UsuarioWeb de(TokensDeKeycloak tokens, PerfilVista perfil) {
    return new UsuarioWeb(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.idToken(),
        Instant.now().plusSeconds(Math.max(1, tokens.expiresIn())),
        perfil
    );
  }

  public UsuarioWeb conTokens(TokensDeKeycloak tokens) {
    return de(tokens, perfil);
  }

  public String autorizacion() {
    return accessToken == null || accessToken.isBlank() ? null : "Bearer " + accessToken;
  }

  public boolean estaPorVencer() {
    return vencimiento != null && Instant.now().isAfter(vencimiento.minus(MARGEN));
  }

  public boolean sePuedeRenovar() {
    return refreshToken != null && !refreshToken.isBlank();
  }
}
