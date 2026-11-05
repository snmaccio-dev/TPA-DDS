package donatrack.web.controller;

import donatrack.web.Modelo;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class DonacionesWebController {

  public void registrarRutas(Javalin app) {
    app.get("/donante-dashboard", this::panelDonante);
    app.get("/mis-donaciones", this::misDonaciones);
    app.get("/entidades", this::entidades);

    app.get("/beneficiario-dashboard", this::panelBeneficiaria);
    app.get("/necesidades", this::necesidades);
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
            .comoDonante().activa("entidades").mapa());
  }

  private void panelBeneficiaria(Context ctx) {
    ctx.render("beneficiario-dashboard.html.hbs",
        Modelo.de("Panel de la entidad").seccion("Panel")
            .comoBeneficiaria().activa("panel").mapa());
  }

  private void necesidades(Context ctx) {
    ctx.render("necesidades.html.hbs",
        Modelo.de("Mis necesidades").seccion("Necesidades")
            .comoBeneficiaria().activa("necesidades").mapa());
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
}
