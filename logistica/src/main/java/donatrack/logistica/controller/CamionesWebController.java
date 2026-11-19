package donatrack.logistica.controller;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.seguridad.VerificadorDeTokens;
import donatrack.logistica.seguridad.VerificadorDeTokens.TokenVerificado;
import donatrack.logistica.service.GestorCamiones;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class CamionesWebController {

    private final GestorCamiones gestor;
    private final VerificadorDeTokens verificador;
    private final boolean seguridadActiva;

    public CamionesWebController(
            GestorCamiones gestor,
            VerificadorDeTokens verificador,
            boolean seguridadActiva
    ) {
        this.gestor = gestor;
        this.verificador = verificador;
        this.seguridadActiva = seguridadActiva;
    }

    public void registrarRutas(Javalin app) {
        app.post("/admin/camiones/sesion", this::iniciarSesion, RolDeAcceso.PUBLICO);
        app.get("/admin/camiones", this::listar, RolDeAcceso.PUBLICO);
        app.post("/admin/camiones", this::crear, RolDeAcceso.PUBLICO);
        app.get("/admin/camiones/{patente}/editar", this::editar, RolDeAcceso.PUBLICO);
        app.post("/admin/camiones/{patente}", this::actualizar, RolDeAcceso.PUBLICO);
        app.post("/admin/camiones/{patente}/eliminar", this::eliminar, RolDeAcceso.PUBLICO);
    }

    private void iniciarSesion(Context ctx) {
        String token = ctx.formParam("token");

        if (seguridadActiva) {
            if (token == null || token.isBlank()) {
                ctx.status(401).result("No se recibió el token de acceso.");
                return;
            }

            try {
                TokenVerificado verificado =
                        verificador.verificar("Bearer " + token);

                if (!verificado.roles().contains(RolDeAcceso.ADMINISTRADOR)) {
                    ctx.status(403).result("No tenés permisos para administrar camiones.");
                    return;
                }

                ctx.sessionAttribute("tokenCamiones", token);

            } catch (Exception e) {
                ctx.status(401).result("La sesión no es válida. Volvé a ingresar desde la aplicación.");
                return;
            }
        }

        ctx.redirect("/admin/camiones");
    }

    private boolean autorizado(Context ctx) {
        if (!seguridadActiva) {
            return true;
        }

        String token = ctx.sessionAttribute("tokenCamiones");

        if (token == null || token.isBlank()) {
            ctx.status(401).result("Sesión vencida. Volvé a ingresar desde la aplicación.");
            return false;
        }

        try {
            TokenVerificado verificado =
                    verificador.verificar("Bearer " + token);

            if (!verificado.roles().contains(RolDeAcceso.ADMINISTRADOR)) {
                ctx.status(403).result("No tenés permisos para acceder.");
                return false;
            }

            return true;

        } catch (Exception e) {
            ctx.sessionAttribute("tokenCamiones", null);
            ctx.status(401).result("Sesión vencida. Volvé a ingresar desde la aplicación.");
            return false;
        }
    }

    private void listar(Context ctx) {
        if (!autorizado(ctx)) return;

        List<Camion> camiones = gestor.todas();

        StringBuilder filas = new StringBuilder();

        for (Camion camion : camiones) {
            filas.append("""
                <tr>
                    <td>%s</td>
                    <td>%.2f m³</td>
                    <td>%.2f m</td>
                    <td>%.2f kg</td>
                    <td>
                        <a href="/admin/camiones/%s/editar">Editar</a>
                        <form method="post" action="/admin/camiones/%s/eliminar"
                              style="display:inline"
                              onsubmit="return confirm('¿Eliminar este camión?')">
                            <button type="submit">Eliminar</button>
                        </form>
                    </td>
                </tr>
                """.formatted(
                    escapar(camion.getPatente()),
                    camion.getCapacidadVolumen(),
                    camion.getAltura(),
                    camion.getCapacidadCarga(),
                    escapar(camion.getPatente()),
                    escapar(camion.getPatente())
            ));
        }

        if (camiones.isEmpty()) {
            filas.append("<tr><td colspan='5'>Todavía no hay camiones registrados.</td></tr>");
        }

        ctx.html(pagina("Administración de camiones", """
            <h1>Administración de camiones</h1>
            <p>Gestioná la flota del sistema.</p>

            <section class="panel">
                <h2>Registrar camión</h2>
                <form method="post" action="/admin/camiones">
                    <label>Patente</label>
                    <input name="patente" required maxlength="20">

                    <label>Capacidad de volumen (m³)</label>
                    <input name="capacidadVolumen" type="number" min="0.01" step="0.01" required>

                    <label>Altura (m)</label>
                    <input name="altura" type="number" min="0.01" step="0.01" required>

                    <label>Capacidad de carga (kg)</label>
                    <input name="capacidadCarga" type="number" min="0.01" step="0.01" required>

                    <button type="submit">Registrar camión</button>
                </form>
            </section>

            <section class="panel">
                <h2>Camiones registrados</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Patente</th>
                            <th>Volumen</th>
                            <th>Altura</th>
                            <th>Carga</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
            </section>
            """.formatted(filas)));
    }

    private void crear(Context ctx) {
        if (!autorizado(ctx)) return;

        try {
            String patente = ctx.formParam("patente");
            double volumen = Double.parseDouble(ctx.formParam("capacidadVolumen"));
            double altura = Double.parseDouble(ctx.formParam("altura"));
            double carga = Double.parseDouble(ctx.formParam("capacidadCarga"));

            gestor.crear(new Camion(patente, volumen, altura, carga));

            ctx.redirect("/admin/camiones");

        } catch (Exception e) {
            mostrarError(ctx, "No se pudo registrar el camión: " + e.getMessage());
        }
    }

    private void editar(Context ctx) {
        if (!autorizado(ctx)) return;

        try {
            Camion camion = gestor.buscar(ctx.pathParam("patente"));

            ctx.html(pagina("Editar camión", """
                <h1>Editar camión</h1>

                <section class="panel">
                    <form method="post" action="/admin/camiones/%s">
                        <label>Patente</label>
                        <input value="%s" disabled>

                        <label>Capacidad de volumen (m³)</label>
                        <input name="capacidadVolumen" type="number" min="0.01" step="0.01"
                               value="%s" required>

                        <label>Altura (m)</label>
                        <input name="altura" type="number" min="0.01" step="0.01"
                               value="%s" required>

                        <label>Capacidad de carga (kg)</label>
                        <input name="capacidadCarga" type="number" min="0.01" step="0.01"
                               value="%s" required>

                        <button type="submit">Guardar cambios</button>
                        <a href="/admin/camiones">Cancelar</a>
                    </form>
                </section>
                """.formatted(
                    escapar(camion.getPatente()),
                    escapar(camion.getPatente()),
                    camion.getCapacidadVolumen(),
                    camion.getAltura(),
                    camion.getCapacidadCarga()
            )));

        } catch (Exception e) {
            mostrarError(ctx, e.getMessage());
        }
    }

    private void actualizar(Context ctx) {
        if (!autorizado(ctx)) return;

        try {
            String patente = ctx.pathParam("patente");

            double volumen = Double.parseDouble(ctx.formParam("capacidadVolumen"));
            double altura = Double.parseDouble(ctx.formParam("altura"));
            double carga = Double.parseDouble(ctx.formParam("capacidadCarga"));

            gestor.actualizar(patente, volumen, altura, carga);

            ctx.redirect("/admin/camiones");

        } catch (Exception e) {
            mostrarError(ctx, "No se pudo actualizar el camión: " + e.getMessage());
        }
    }

    private void eliminar(Context ctx) {
        if (!autorizado(ctx)) return;

        try {
            gestor.eliminar(ctx.pathParam("patente"));
            ctx.redirect("/admin/camiones");

        } catch (Exception e) {
            mostrarError(ctx, "No se pudo eliminar el camión: " + e.getMessage());
        }
    }

    private void mostrarError(Context ctx, String mensaje) {
        ctx.status(400);
        ctx.html(pagina("Error", """
            <h1>No se pudo completar la operación</h1>
            <p>%s</p>
            <a href="/admin/camiones">Volver a camiones</a>
            """.formatted(escapar(mensaje))));
    }

    private String pagina(String titulo, String contenido) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        padding: 40px;
                        font-family: Arial, sans-serif;
                        background: #f3f5f8;
                        color: #202938;
                    }
                    main { max-width: 1100px; margin: auto; }
                    .panel {
                        background: white;
                        padding: 25px;
                        border-radius: 12px;
                        margin: 25px 0;
                        box-shadow: 0 2px 12px #0000000d;
                    }
                    label { display: block; margin-top: 15px; }
                    input {
                        width: 100%%;
                        max-width: 450px;
                        padding: 11px;
                        margin-top: 6px;
                        border: 1px solid #ccd2dc;
                        border-radius: 6px;
                    }
                    button {
                        padding: 11px 18px;
                        margin-top: 18px;
                        border: 0;
                        border-radius: 6px;
                        background: #2457c5;
                        color: white;
                        cursor: pointer;
                    }
                    table { width: 100%%; border-collapse: collapse; }
                    th, td {
                        padding: 13px;
                        border-bottom: 1px solid #e5e8ee;
                        text-align: left;
                    }
                    a { color: #2457c5; margin-right: 12px; }
                </style>
            </head>
            <body>
                <main>
                    %s
                    <p><a href="http://localhost:8082/">Volver a la aplicación</a></p>
                </main>
            </body>
            </html>
            """.formatted(escapar(titulo), contenido);
    }

    private String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
