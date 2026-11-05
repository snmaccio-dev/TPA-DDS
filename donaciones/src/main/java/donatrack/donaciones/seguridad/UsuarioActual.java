package donatrack.donaciones.seguridad;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;

import java.util.Optional;
import java.util.Set;

// Snapshot del usuario autenticado para la request en curso.
// Los perfiles (donante / beneficiaria) estan en Optional porque un
// ADMINISTRADOR o un CHOFER no tienen fila local, pero si pasaron el filtro.
public record UsuarioActual(
    String keycloakId,
    String email,
    Set<RolDeAcceso> roles,
    Optional<Donante> donante,
    Optional<Beneficiaria> beneficiaria
) {

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

    public boolean esServicioLogistica() {
        return tieneRol(RolDeAcceso.SERVICIO_LOGISTICA);
    }
}