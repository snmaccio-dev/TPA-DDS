package donatrack.logistica.controller;

import donatrack.logistica.domain.ConflictoDeNegocioException;
import donatrack.logistica.domain.ErrorDeDominio;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.seguridad.FiltroDeAcceso;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.seguridad.UsuarioActual;
import donatrack.logistica.service.GestorCamiones;
import donatrack.logistica.service.RecursoInexistenteException;
import donatrack.logistica.vista.CamionVista;
import donatrack.logistica.vista.FlotaVista;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pantalla de administracion de camiones. El login lo resuelve SesionCamionesController y
// el control de acceso FiltroDeAcceso, con el token guardado en la sesion.
public class CamionesWebController {

  private final GestorCamiones gestor;
  private final String urlWeb;

  public CamionesWebController(GestorCamiones gestor, String urlWeb) {
    this.gestor = gestor;
    this.urlWeb = urlWeb;
  }

  public void registrarRutas(Javalin app) {
    app.get("/admin/camiones", this::listar, RolDeAcceso.ADMINISTRADOR);
    app.post("/admin/camiones", this::crear, RolDeAcceso.ADMINISTRADOR);
    app.get("/admin/camiones/{patente}/editar", this::editar, RolDeAcceso.ADMINISTRADOR);
    app.post("/admin/camiones/{patente}", this::actualizar, RolDeAcceso.ADMINISTRADOR);
    app.post("/admin/camiones/{patente}/eliminar", this::eliminar, RolDeAcceso.ADMINISTRADOR);
  }

  private void listar(Context ctx) {
    mostrarFlota(ctx, null);
  }

  private void crear(Context ctx) {
    try {
      gestor.crear(new Camion(
          ctx.formParam("patente"),
          decimal(ctx, "capacidadVolumen"),
          decimal(ctx, "altura"),
          decimal(ctx, "capacidadCarga")
      ));
    } catch (ErrorDeDominio | RecursoInexistenteException e) {
      ctx.status(estadoDe(e));
      mostrarFlota(ctx, "No se pudo registrar el camión: " + e.getMessage());
      return;
    }

    ctx.redirect("/admin/camiones");
  }

  private void editar(Context ctx) {
    String patente = ctx.pathParam("patente");

    try {
      Camion camion = gestor.buscar(patente);
      mostrarEdicion(ctx, patente, camion, null);
    } catch (RecursoInexistenteException e) {
      ctx.status(estadoDe(e));
      mostrarEdicion(ctx, patente, null, e.getMessage());
    }
  }

  private void actualizar(Context ctx) {
    String patente = ctx.pathParam("patente");

    try {
      gestor.actualizar(
          patente,
          decimal(ctx, "capacidadVolumen"),
          decimal(ctx, "altura"),
          decimal(ctx, "capacidadCarga")
      );
    } catch (ErrorDeDominio | RecursoInexistenteException e) {
      ctx.status(estadoDe(e));
      mostrarEdicion(ctx, patente, camionSiExiste(patente),
          "No se pudo actualizar el camión: " + e.getMessage());
      return;
    }

    ctx.redirect("/admin/camiones");
  }

  private void eliminar(Context ctx) {
    try {
      gestor.eliminar(ctx.pathParam("patente"));
    } catch (ErrorDeDominio | RecursoInexistenteException e) {
      ctx.status(estadoDe(e));
      mostrarFlota(ctx, "No se pudo eliminar el camión: " + e.getMessage());
      return;
    }

    ctx.redirect("/admin/camiones");
  }

  private void mostrarFlota(Context ctx, String alertaError) {
    Set<String> enRuta = gestor.patentesEnRuta();
    List<CamionVista> camiones = gestor.todas().stream()
        .map(camion -> CamionVista.desde(camion, enRuta.contains(camion.getPatente())))
        .toList();

    Map<String, Object> modelo = modelo(ctx, "Camiones", alertaError);
    modelo.put("flota", new FlotaVista(camiones));

    ctx.render("camiones.html.hbs", modelo);
  }

  private void mostrarEdicion(Context ctx, String patente, Camion camion, String alertaError) {
    Map<String, Object> modelo = modelo(ctx, "Editar camión", alertaError);
    modelo.put("patente", patente);

    if (camion != null) {
      modelo.put("camion", CamionVista.desde(
          camion, gestor.patentesEnRuta().contains(camion.getPatente())));
    }

    ctx.render("camion-editar.html.hbs", modelo);
  }

  private Map<String, Object> modelo(Context ctx, String titulo, String alertaError) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("titulo", titulo);
    modelo.put("seccion", "Camiones");
    modelo.put("urlWeb", urlWeb);
    modelo.put("usuario", usuario(ctx));

    if (alertaError != null) {
      modelo.put("alertaError", alertaError);
    }

    return modelo;
  }

  // Sin seguridad (LOGISTICA_SEGURIDAD=off) no hay usuario autenticado en el contexto.
  private static Map<String, String> usuario(Context ctx) {
    String nombre = FiltroDeAcceso.usuarioSiHay(ctx)
        .map(UsuarioActual::email)
        .filter(email -> !email.isBlank())
        .orElse("Administración");

    return Map.of(
        "nombre", nombre,
        "iniciales", nombre.substring(0, Math.min(2, nombre.length())).toUpperCase()
    );
  }

  private Camion camionSiExiste(String patente) {
    try {
      return gestor.buscar(patente);
    } catch (RecursoInexistenteException e) {
      return null;
    }
  }

  private static double decimal(Context ctx, String campo) {
    String valor = ctx.formParam(campo);

    try {
      return Double.parseDouble(valor == null ? "" : valor.trim());
    } catch (NumberFormatException e) {
      throw new DatosInvalidosException("El campo " + campo + " tiene que ser un número.");
    }
  }

  private static int estadoDe(RuntimeException e) {
    if (e instanceof RecursoInexistenteException) return 404;
    if (e instanceof ConflictoDeNegocioException) return 409;
    return 400;
  }
}
