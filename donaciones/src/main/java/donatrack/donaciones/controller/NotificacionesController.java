package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.NotificacionDTO;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.ConsultaDeNotificaciones;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class NotificacionesController {

    private static final int TAMANIO_POR_DEFECTO = 20;
    private static final int TAMANIO_MAXIMO = 100;

    private final ConsultaDeNotificaciones consulta;

    public NotificacionesController(ConsultaDeNotificaciones consulta) {
        this.consulta = consulta;
    }

    public void registrarRutas(Javalin app) {
        app.get("/notificaciones/mias", this::mias,
            RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA, RolDeAcceso.ADMINISTRADOR);
        app.get("/notificaciones/mias/no-leidas/cantidad", this::cantidadNoLeidas,
            RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA, RolDeAcceso.ADMINISTRADOR);
        app.patch("/notificaciones/{id}/leida", this::marcarLeida,
            RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA, RolDeAcceso.ADMINISTRADOR);
        app.post("/notificaciones/mias/leidas", this::marcarTodasLeidas,
            RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA, RolDeAcceso.ADMINISTRADOR);
        app.get("/notificaciones", this::todas, RolDeAcceso.ADMINISTRADOR);
    }

    private void mias(Context ctx) {
        UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
        boolean soloNoLeidas = booleanParam(ctx, "soloNoLeidas", false);
        int pagina = intParam(ctx, "pagina", 0);
        int tamanio = tamanioEfectivo(intParam(ctx, "tamanio", TAMANIO_POR_DEFECTO));
        ctx.json(NotificacionDTO.desde(consulta.propiasDe(usuario, soloNoLeidas, pagina, tamanio)));
    }

    private void cantidadNoLeidas(Context ctx) {
        UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
        ctx.json(new CantidadResponse(consulta.cantidadNoLeidasDe(usuario)));
    }

    private void marcarLeida(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Notificacion notificacion = consulta.buscarPorId(id);
        Acceso.exigirPuedeVerNotificacion(FiltroDeAcceso.usuarioDe(ctx), notificacion);
        consulta.marcarLeida(notificacion);
        ctx.status(204);
    }

    private void marcarTodasLeidas(Context ctx) {
        UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
        consulta.marcarTodasLeidasDe(usuario);
        ctx.status(204);
    }

    private void todas(Context ctx) {
        EstadoEnvio estadoEnvio = enumParam(ctx, "estadoEnvio", EstadoEnvio.class);
        TipoNotificacion tipo = enumParam(ctx, "tipo", TipoNotificacion.class);
        int pagina = intParam(ctx, "pagina", 0);
        int tamanio = tamanioEfectivo(intParam(ctx, "tamanio", TAMANIO_POR_DEFECTO));
        ctx.json(NotificacionDTO.desde(consulta.todas(estadoEnvio, tipo, pagina, tamanio)));
    }

    // === Helpers ===

    private static int tamanioEfectivo(int tamanio) {
        if (tamanio <= 0) {
            return TAMANIO_POR_DEFECTO;
        }
        return Math.min(tamanio, TAMANIO_MAXIMO);
    }

    private static boolean booleanParam(Context ctx, String nombre, boolean porDefecto) {
        String raw = ctx.queryParam(nombre);
        if (raw == null || raw.isBlank()) {
            return porDefecto;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private static int intParam(Context ctx, String nombre, int porDefecto) {
        String raw = ctx.queryParam(nombre);
        if (raw == null || raw.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(
                "El query param " + nombre + " debe ser un entero.");
        }
    }

    private static <E extends Enum<E>> E enumParam(Context ctx, String nombre, Class<E> clase) {
        String raw = ctx.queryParam(nombre);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(clase, raw);
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(
                "El query param " + nombre + " tiene un valor invalido: " + raw);
        }
    }

    public record CantidadResponse(long cantidad) {}
}
