package donatrack.logistica.seguridad;

import io.javalin.security.RouteRole;

import java.util.Optional;

public enum RolDeAcceso implements RouteRole {
  PUBLICO,
  DONANTE,
  BENEFICIARIA,
  ADMINISTRADOR,
  CHOFER,
  SERVICIO_LOGISTICA;

  public static Optional<RolDeAcceso> desdeNombre(String nombre) {
    if (nombre == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(RolDeAcceso.valueOf(nombre));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
