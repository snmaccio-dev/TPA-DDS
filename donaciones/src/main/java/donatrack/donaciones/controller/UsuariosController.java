package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.UsuarioActualDTO;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import io.javalin.http.Context;

public class UsuariosController {

    public void yo(Context ctx) {
        UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
        ctx.json(UsuarioActualDTO.desde(usuario));
    }
}