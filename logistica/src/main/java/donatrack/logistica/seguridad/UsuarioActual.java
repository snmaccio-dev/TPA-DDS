package donatrack.logistica.seguridad;

import java.util.Set;

public record UsuarioActual(
    String keycloakId,
    String email,
    Set<RolDeAcceso> roles
) {

  public UsuarioActual {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  public boolean tieneRol(RolDeAcceso rol) {
    return roles.contains(rol);
  }

  public boolean esAdministrador() {
    return tieneRol(RolDeAcceso.ADMINISTRADOR);
  }

  public boolean esDonante() {
    return tieneRol(RolDeAcceso.DONANTE);
  }

  public boolean esBeneficiaria() {
    return tieneRol(RolDeAcceso.BENEFICIARIA);
  }

  public boolean esChofer() {
    return tieneRol(RolDeAcceso.CHOFER);
  }

  public boolean esServicioLogistica() {
    return tieneRol(RolDeAcceso.SERVICIO_LOGISTICA);
  }
}
