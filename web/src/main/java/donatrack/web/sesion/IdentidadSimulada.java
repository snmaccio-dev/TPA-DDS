package donatrack.web.sesion;

import io.javalin.http.Context;

import java.util.Optional;

public class IdentidadSimulada implements IdentidadDelUsuario {

  @Override
  public Optional<UsuarioWeb> actual(Context ctx) {
    return Optional.of(UsuarioWeb.deDemostracion());
  }
}
