package donatrack.logistica.seguridad;

import donatrack.logistica.seguridad.web.TokensDeKeycloak;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class FiltroDeAcceso {

  private static final String CLAVE_USUARIO = "usuarioActual";

  private static final String PANTALLA_DE_CAMIONES = "/admin/camiones";
  private static final String INGRESO_A_CAMIONES = "/admin/camiones/ingresar";

  private final VerificadorDeTokens verificador;

  public FiltroDeAcceso(VerificadorDeTokens verificador) {
    Objects.requireNonNull(verificador, "Debe indicarse el verificador de tokens.");
    this.verificador = verificador;
  }

  public void registrarEn(Javalin app) {
    app.beforeMatched(this::aplicar);
  }

  public void aplicar(Context ctx) {
    // beforeMatched tambien corre para los archivos estaticos, que no declaran roles: sin
    // esto respondian 403. Los assets de la pantalla de camiones y las fotos de las
    // entregas (que muestran la landing y la pagina publica de donaciones) son publicos.
    if (ctx.path().startsWith("/assets/") || ctx.path().startsWith("/fotos/")) {
      return;
    }

    Set<? extends RouteRole> roles = ctx.routeRoles();

    if (roles == null || roles.isEmpty()) {
      System.err.println("[SEGURIDAD] Ruta sin roles declarados: "
          + ctx.method() + " " + ctx.path());
      throw new ForbiddenResponse("La ruta no declaro roles de acceso.");
    }

    if (roles.contains(RolDeAcceso.PUBLICO)) {
      return;
    }

    String autorizacion = ctx.header("Authorization");

    // La pantalla de camiones la usa un navegador, que no manda header Authorization:
    // el token sale de la sesion. La API REST sigue exigiendo el header.
    if (autorizacion == null && esPantallaDeCamiones(ctx.path())) {
      aplicarConSesion(ctx, roles);
      return;
    }

    exigirRoles(ctx, verificador.verificar(autorizacion), roles);
  }

  private void aplicarConSesion(Context ctx, Set<? extends RouteRole> roles) {
    HttpSession sesion = ctx.req().getSession(false);
    TokensDeKeycloak tokens = sesion == null
        ? null
        : (TokensDeKeycloak) sesion.getAttribute(TokensDeKeycloak.CLAVE_SESION);

    if (tokens == null) {
      redirigirAlIngreso(ctx, sesion);
      return;
    }

    VerificadorDeTokens.TokenVerificado token;
    try {
      token = verificador.verificar("Bearer " + tokens.accessToken());
    } catch (UnauthorizedResponse e) {
      // Vencido o invalido. No hay refresh token: se vuelve a pasar por Keycloak, que
      // con la sesion SSO activa devuelve un token nuevo sin pedir credenciales.
      redirigirAlIngreso(ctx, sesion);
      return;
    }

    exigirRoles(ctx, token, roles);
  }

  private static void redirigirAlIngreso(Context ctx, HttpSession sesion) {
    if (sesion != null) {
      sesion.removeAttribute(TokensDeKeycloak.CLAVE_SESION);
    }
    ctx.redirect(INGRESO_A_CAMIONES);
    ctx.skipRemainingHandlers();
  }

  private static void exigirRoles(Context ctx, VerificadorDeTokens.TokenVerificado token,
                                  Set<? extends RouteRole> roles) {
    if (token.roles().stream().noneMatch(roles::contains)) {
      throw new ForbiddenResponse(
          "La cuenta no tiene los roles necesarios para esta operacion."
      );
    }

    ctx.attribute(CLAVE_USUARIO, new UsuarioActual(
        token.keycloakId(),
        token.email(),
        token.roles()
    ));
  }

  private static boolean esPantallaDeCamiones(String path) {
    return path.equals(PANTALLA_DE_CAMIONES) || path.startsWith(PANTALLA_DE_CAMIONES + "/");
  }

  public static UsuarioActual usuarioDe(Context ctx) {
    return usuarioSiHay(ctx).orElseThrow(() ->
        new ForbiddenResponse("No hay usuario autenticado en el contexto."));
  }

  // Vacio cuando la seguridad esta apagada (LOGISTICA_SEGURIDAD=off).
  public static Optional<UsuarioActual> usuarioSiHay(Context ctx) {
    UsuarioActual actual = ctx.attribute(CLAVE_USUARIO);
    return Optional.ofNullable(actual);
  }
}
