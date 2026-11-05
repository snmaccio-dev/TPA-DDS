package donatrack.web.sesion;

import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public interface IdentidadDelUsuario {

  Optional<Long> beneficiaria(Context ctx);

  Optional<List<Long>> donaciones(Context ctx);

  Optional<Long> entregaAConfirmar(Context ctx);

  String comoIdentificarse();
}
