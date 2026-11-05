package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.UsuarioActualDTO;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class UsuariosController {

    public void registrarRutas(Javalin app) {
        app.get("/usuarios/yo", this::yo,
            RolDeAcceso.DONANTE,
            RolDeAcceso.BENEFICIARIA,
            RolDeAcceso.ADMINISTRADOR,
            RolDeAcceso.CHOFER);
    }

    private void yo(Context ctx) {
        UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
        ctx.json(UsuarioActualDTO.desde(usuario));
    }
}