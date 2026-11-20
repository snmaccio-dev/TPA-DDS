package donatrack.logistica.controller;

import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.seguridad.VerificadorDeTokens;
import donatrack.logistica.seguridad.VerificadorDeTokens.TokenVerificado;
import donatrack.logistica.seguridad.web.ClienteOidc;
import donatrack.logistica.seguridad.web.Pkce;
import donatrack.logistica.seguridad.web.TokensDeKeycloak;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

// Login OIDC (authorization code + PKCE) propio de la pantalla de camiones, contra el
// client publico "logistica-web". Si el usuario ya inicio sesion en web, el SSO de
// Keycloak lo devuelve al callback sin volver a pedirle credenciales.
//
// Estas rutas se registran antes que las de CamionesWebController: POST
// /admin/camiones/salir coincidiria tambien con POST /admin/camiones/{patente}.
public class SesionCamionesController {

  private static final String CLAVE_ESTADO = "estadoDeIngreso";
  private static final String CLAVE_VERIFICADOR = "verificadorDeIngreso";

  private static final SecureRandom AZAR = new SecureRandom();
  private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

  private final ClienteOidc oidc;
  private final VerificadorDeTokens verificador;
  private final String urlWeb;

  public SesionCamionesController(ClienteOidc oidc, VerificadorDeTokens verificador,
                                  String urlWeb) {
    this.oidc = oidc;
    this.verificador = verificador;
    this.urlWeb = urlWeb;
  }

  public void registrarRutas(Javalin app) {
    app.get("/admin/camiones/ingresar", this::ingresar, RolDeAcceso.PUBLICO);
    app.get("/admin/camiones/callback", this::recibirCodigo, RolDeAcceso.PUBLICO);
    app.post("/admin/camiones/salir", this::salir, RolDeAcceso.ADMINISTRADOR);
  }

  private void ingresar(Context ctx) {
    String estado = nuevoValorAlAzar();
    String verificadorPkce = Pkce.nuevoVerificador();

    ctx.sessionAttribute(CLAVE_ESTADO, estado);
    ctx.sessionAttribute(CLAVE_VERIFICADOR, verificadorPkce);

    ctx.redirect(oidc.urlDeIngreso(estado, Pkce.desafioDe(verificadorPkce)));
  }

  private void recibirCodigo(Context ctx) {
    String estadoEsperado = ctx.sessionAttribute(CLAVE_ESTADO);
    String verificadorPkce = ctx.sessionAttribute(CLAVE_VERIFICADOR);

    ctx.sessionAttribute(CLAVE_ESTADO, null);
    ctx.sessionAttribute(CLAVE_VERIFICADOR, null);

    String codigo = ctx.queryParam("code");
    String estadoRecibido = ctx.queryParam("state");

    if (codigo == null || estadoEsperado == null || !estadoEsperado.equals(estadoRecibido)) {
      mostrarRechazo(ctx, 400, "El ingreso no se pudo completar. Volvé a intentarlo.");
      return;
    }

    TokensDeKeycloak tokens;
    TokenVerificado token;

    try {
      tokens = oidc.intercambiar(codigo, verificadorPkce);
      token = verificador.verificar("Bearer " + tokens.accessToken());
    } catch (IntegracionExternaException | UnauthorizedResponse e) {
      mostrarRechazo(ctx, 400, "El ingreso no se pudo completar. Volvé a intentarlo.");
      return;
    }

    if (!token.roles().contains(RolDeAcceso.ADMINISTRADOR)) {
      mostrarRechazo(ctx, 403,
          "Tu cuenta no tiene permisos para administrar camiones.");
      return;
    }

    // Contra fijacion de sesion: el usuario queda autenticado con un id de sesion nuevo.
    ctx.req().changeSessionId();
    ctx.sessionAttribute(TokensDeKeycloak.CLAVE_SESION, tokens);

    ctx.redirect("/admin/camiones");
  }

  private void salir(Context ctx) {
    TokensDeKeycloak tokens = ctx.sessionAttribute(TokensDeKeycloak.CLAVE_SESION);

    HttpSession sesion = ctx.req().getSession(false);
    if (sesion != null) {
      sesion.invalidate();
    }

    ctx.redirect(oidc.urlDeSalida(tokens == null ? null : tokens.idToken()));
  }

  private void mostrarRechazo(Context ctx, int estado, String mensaje) {
    ctx.status(estado).render("acceso-denegado.html.hbs", Map.of(
        "titulo", "Acceso denegado",
        "urlWeb", urlWeb,
        "codigo", estado,
        "mensaje", mensaje
    ));
  }

  private static String nuevoValorAlAzar() {
    byte[] bytes = new byte[24];
    AZAR.nextBytes(bytes);

    return BASE64.encodeToString(bytes);
  }
}
