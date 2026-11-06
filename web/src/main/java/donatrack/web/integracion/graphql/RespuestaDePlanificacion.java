package donatrack.web.integracion.graphql;

import java.util.List;

public record RespuestaDePlanificacion(
    DatosDePlanificacion data,
    List<ErrorGraphQl> errors
) {

  public boolean tieneErrores() {
    return errors != null && !errors.isEmpty();
  }

  public String primerError() {
    return tieneErrores() ? errors.get(0).message() : null;
  }
}
