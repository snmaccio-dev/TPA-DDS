package donatrack.logistica.seguridad;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.security.RouteRole;

import java.util.Objects;
import java.util.Set;

public class FiltroDeAcceso {

  private static final String CLAVE_USUARIO = "usuarioActual";

  private final VerificadorDeTokens verificador;

  public FiltroDeAcceso(VerificadorDeTokens verificador) {
    Objects.requireNonNull(verificador, "Debe indicarse el verificador de tokens.");
    this.verificador = verificador;
  }

  public void registrarEn(Javalin app) {
    app.beforeMatched(this::aplicar);
  }

  public void aplicar(Context ctx) {
    Set<? extends RouteRole> roles = ctx.routeRoles();

    if (roles == null || roles.isEmpty()) {
      System.err.println("[SEGURIDAD] Ruta sin roles declarados: "
          + ctx.method() + " " + ctx.path());
      throw new ForbiddenResponse("La ruta no declaro roles de acceso.");
    }

    if (roles.contains(RolDeAcceso.PUBLICO)) {
      return;
    }

    VerificadorDeTokens.TokenVerificado token =
        verificador.verificar(ctx.header("Authorization"));

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

  public static UsuarioActual usuarioDe(Context ctx) {
    UsuarioActual actual = ctx.attribute(CLAVE_USUARIO);

    if (actual == null) {
      throw new ForbiddenResponse("No hay usuario autenticado en el contexto.");
    }

    return actual;
  }
}
