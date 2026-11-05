package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.NuevoAdministradorRequest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.javalin.Javalin;
import io.javalin.http.Context;

// Alta de administradores: solo crea la cuenta en Keycloak, no persiste nada
// local. Un administrador no necesita fila: alcanza con tener el rol en el
// token para pasar el FiltroDeAcceso.
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
        String keycloakId = proveedor.crearUsuarioSinContrasena(request.email(), RolDeAcceso.ADMINISTRADOR);
        ctx.status(201).json(new NuevoAdministradorResponse(keycloakId));
    }

    public record NuevoAdministradorResponse(String keycloakId) {}
}