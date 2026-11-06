package donatrack.web.controller;

import donatrack.web.Modelo;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.NuevaNecesidad;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import io.javalin.Javalin;
import io.javalin.http.InternalServerErrorResponse;

import java.util.List;
import io.javalin.http.Context;

public class DonacionesWebController {

  private final ClienteDonaciones clienteDonaciones;
  private final IdentidadDelUsuario identidad;

  public DonacionesWebController(ClienteDonaciones clienteDonaciones,
                                 IdentidadDelUsuario identidad) {
    this.clienteDonaciones = clienteDonaciones;
    this.identidad = identidad;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donante-dashboard", this::panelDonante);
    app.get("/mis-donaciones", this::misDonaciones);
    app.get("/entidades", this::entidades);

    app.get("/beneficiario-dashboard", this::panelBeneficiaria);
    app.get("/necesidades", this::necesidades);
    app.post("/necesidades", this::crearNecesidad);
    app.get("/donaciones-asignadas", this::donacionesAsignadas);

    app.get("/admin-dashboard", this::panelAdmin);
    app.get("/admin-donantes", this::donantes);
    app.get("/deposito", this::deposito);
    app.get("/asignaciones", this::asignaciones);
    app.get("/importar-csv", this::importarCsv);
  }

  private void panelDonante(Context ctx) {
    ctx.render("donante-dashboard.html.hbs",
        Modelo.de("Panel de donante").seccion("Panel").comoDonante().activa("panel").mapa());
  }

  private void misDonaciones(Context ctx) {
    ctx.render("mis-donaciones.html.hbs",
        Modelo.de("Mis donaciones").seccion("Mis donaciones")
            .comoDonante().activa("misDonaciones").mapa());
  }

  private void entidades(Context ctx) {
    ctx.render("entidades.html.hbs",
        Modelo.de("Entidades").seccion("Entidades")
            .conUsuario(usuario(ctx).perfil())
            .comoDonante().activa("entidades")
            .conDatosDe("entidades", List.<EntidadVista>of(),
                () -> clienteDonaciones.entidades(autorizacionDe(ctx)))
            .mapa());
  }

  private void panelBeneficiaria(Context ctx) {
    ctx.render("beneficiario-dashboard.html.hbs",
        Modelo.de("Panel de la entidad").seccion("Panel")
            .comoBeneficiaria().activa("panel").mapa());
  }

  private void necesidades(Context ctx) {
    mostrarNecesidades(ctx, null);
  }

  private void crearNecesidad(Context ctx) {
    // El cuit va en null: Donaciones resuelve la entidad desde el token de quien llama.
    NuevaNecesidad nueva = new NuevaNecesidad(
        null,
        ctx.formParam("descripcion"),
        ctx.formParamAsClass("cantidad", Integer.class).getOrDefault(0),
        ctx.formParam("subcategoria"),
        ctx.formParam("tipo"),
        ctx.formParam("periodo")
    );

    try {
      clienteDonaciones.crearNecesidad(autorizacionDe(ctx), nueva);
    } catch (OperacionRechazadaException e) {
      mostrarNecesidades(ctx, e.getMessage());
      return;
    }

    ctx.redirect("/necesidades");
  }

  private void mostrarNecesidades(Context ctx, String errorDelFormulario) {
    ctx.render("necesidades.html.hbs",
        Modelo.de("Mis necesidades").seccion("Necesidades")
            .conUsuario(usuario(ctx).perfil())
            .comoBeneficiaria().activa("necesidades")
            .con("errorDelFormulario", errorDelFormulario)
            .conDatosDe("necesidades", List.<NecesidadVista>of(),
                () -> clienteDonaciones.necesidades(autorizacionDe(ctx)))
            .mapa());
  }

  private void donacionesAsignadas(Context ctx) {
    ctx.render("donaciones-asignadas.html.hbs",
        Modelo.de("Donaciones asignadas").seccion("Donaciones asignadas")
            .comoBeneficiaria().activa("asignadas").mapa());
  }

  private void panelAdmin(Context ctx) {
    ctx.render("admin-dashboard.html.hbs",
        Modelo.de("Panel de administración").seccion("Panel")
            .comoAdmin().activa("panel").mapa());
  }

  private void donantes(Context ctx) {
    ctx.render("admin-donantes.html.hbs",
        Modelo.de("Donantes").seccion("Donantes").comoAdmin().activa("donantes").mapa());
  }

  private void deposito(Context ctx) {
    ctx.render("deposito.html.hbs",
        Modelo.de("Depósito").seccion("Depósito").comoAdmin().activa("deposito").mapa());
  }

  private void asignaciones(Context ctx) {
    ctx.render("asignaciones.html.hbs",
        Modelo.de("Asignaciones").seccion("Asignaciones")
            .comoAdmin().activa("asignaciones").mapa());
  }

  private void importarCsv(Context ctx) {
    ctx.render("importar-csv.html.hbs",
        Modelo.de("Importar CSV").seccion("Importar CSV")
            .comoAdmin().activa("importar").mapa());
  }

  private String autorizacionDe(Context ctx) {
    return usuario(ctx).autorizacion();
  }

  private UsuarioWeb usuario(Context ctx) {
    return identidad.actual(ctx).orElseThrow(() ->
        new InternalServerErrorResponse("No hay una sesion iniciada."));
  }
}
