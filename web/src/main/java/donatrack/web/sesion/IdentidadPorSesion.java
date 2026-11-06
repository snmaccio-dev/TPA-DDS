package donatrack.web.sesion;

import io.javalin.http.Context;

import java.util.Optional;

public class IdentidadPorSesion implements IdentidadDelUsuario {

  public static final String CLAVE_USUARIO = "usuarioDeLaSesion";

  @Override
  public Optional<UsuarioWeb> actual(Context ctx) {
    return Optional.ofNullable(ctx.sessionAttribute(CLAVE_USUARIO));
  }
}
