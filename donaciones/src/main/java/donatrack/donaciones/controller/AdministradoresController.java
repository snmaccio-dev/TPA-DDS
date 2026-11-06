package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.NuevoAdministradorRequest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.seguridad.PoliticaDeContrasenas;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.javalin.Javalin;
import io.javalin.http.Context;

// Alta de administradores: el admin que da de alta define la contrasena
// directamente. La cuenta queda en Keycloak y el admin nuevo puede cambiar
// la contrasena desde la pagina de cuenta del propio Keycloak
// ({KEYCLOAK_URL}/realms/donatrack/account). No se persiste nada local:
// alcanza con tener el rol ADMINISTRADOR en el token para pasar el filtro.
public class AdministradoresController {

    private final ProveedorDeIdentidad proveedor;

    public AdministradoresController(ProveedorDeIdentidad proveedor) {
        this.proveedor = proveedor;
    }

    public void registrarRutas(Javalin app) {
        app.post("/administradores", this::crear, RolDeAcceso.ADMINISTRADOR);
    }

    private void crear(Context ctx) {
        NuevoAdministradorRequest request = ctx.bodyAsClass(NuevoAdministradorRequest.class);
        if (request.email() == null || request.email().isBlank()) {
            throw new DatosInvalidosException("El email del administrador es obligatorio.");
        }
        PoliticaDeContrasenas.validar(request.contrasena());
        // No marcamos la contrasena como temporal. Keycloak, cuando una cuenta
        // tiene la required action UPDATE_PASSWORD pendiente, rechaza el grant
        // de password con "Account is not fully set up", y el login del web
        // deja de funcionar hasta que el usuario pase por un flujo de UI
        // interactivo. Dejar la contrasena como no-temporal evita ese bloqueo;
        // el admin nuevo la cambia despues desde /realms/donatrack/account.
        String keycloakId = proveedor.crearUsuario(
            request.email(), request.contrasena(), RolDeAcceso.ADMINISTRADOR);
        ctx.status(201).json(new NuevoAdministradorResponse(keycloakId));
    }

    public record NuevoAdministradorResponse(String keycloakId) {}
}