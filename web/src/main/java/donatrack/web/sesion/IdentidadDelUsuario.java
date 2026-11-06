package donatrack.web.sesion;

import io.javalin.http.Context;

import java.util.Optional;

public interface IdentidadDelUsuario {

  Optional<UsuarioWeb> actual(Context ctx);
}
