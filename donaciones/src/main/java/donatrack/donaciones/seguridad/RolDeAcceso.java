package donatrack.donaciones.seguridad;

import io.javalin.security.RouteRole;

// Roles de realm en Keycloak que conoce el microservicio Donaciones.
// Si un token trae un rol que no esta en este enum (por ejemplo
// offline_access o default-roles-donatrack) simplemente se ignora.
public enum RolDeAcceso implements RouteRole {
    PUBLICO,
    DONANTE,
    BENEFICIARIA,
    ADMINISTRADOR,
    CHOFER,
    SERVICIO_LOGISTICA;

    public static java.util.Optional<RolDeAcceso> desdeNombre(String nombre) {
        if (nombre == null) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(RolDeAcceso.valueOf(nombre));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}