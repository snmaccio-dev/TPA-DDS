package donatrack.web.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClienteOidcTest {

  private final ClienteOidc oidc = new ClienteOidc(new ConfiguracionOidc(
      "http://localhost:8180", "donatrack", "donatrack-web", "http://localhost:8082"));

  @Test
  void elIngresoNormalNoPideCredencialesNiSugiereUsuario() {
    String url = oidc.urlDeIngreso("estado", "desafio");

    assertFalse(url.contains("prompt="), url);
    assertFalse(url.contains("login_hint="), url);
    assertEquals(url, oidc.urlDeIngreso("estado", "desafio", false, null));
  }

  @Test
  void despuesDeUnRegistroPideElFormularioYCompletaElCorreo() {
    String url = oidc.urlDeIngreso("estado", "desafio", true, " ana+demo@mail.com ");

    assertTrue(url.contains("&prompt=login"), url);
    assertTrue(url.contains("&login_hint=ana%2Bdemo%40mail.com"), url);
    assertTrue(url.contains("&code_challenge=desafio"), url);
  }

  @Test
  void sinCorreoSoloPideElFormulario() {
    String url = oidc.urlDeIngreso("estado", "desafio", true, null);

    assertTrue(url.contains("&prompt=login"), url);
    assertFalse(url.contains("login_hint="), url);
  }
}
