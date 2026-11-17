package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.NuevoAdministradorRequest;
import donatrack.donaciones.service.UsuariosService;
import io.javalin.http.Context;

// Alta de administradores: el admin que da de alta define la contrasena
// directamente. La cuenta queda en Keycloak y el admin nuevo puede cambiar
// la contrasena desde la pagina de cuenta del propio Keycloak
// ({KEYCLOAK_URL}/realms/donatrack/account).
public class AdministradoresController {

    private final UsuariosService usuariosService;

    public AdministradoresController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    public void crear(Context ctx) {
        NuevoAdministradorRequest request = ctx.bodyAsClass(NuevoAdministradorRequest.class);
        String keycloakId = usuariosService.crearAdministrador(request.email(), request.contrasena());
        ctx.status(201).json(new NuevoAdministradorResponse(keycloakId));
    }

    public record NuevoAdministradorResponse(String keycloakId) {}
}
