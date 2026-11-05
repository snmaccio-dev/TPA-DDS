package donatrack.web.controller;

import donatrack.web.Modelo;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class LogisticaWebController {

  public void registrarRutas(Javalin app) {
    app.get("/camiones", this::camiones);
    app.get("/seguimiento-donante", this::seguimientoDonante);
    app.get("/seguimiento-beneficiario", this::seguimientoBeneficiaria);
    app.get("/confirmar-recepcion", this::confirmarRecepcion);
  }

  private void camiones(Context ctx) {
    ctx.render("camiones.html.hbs",
        Modelo.de("Camiones").seccion("Camiones").comoAdmin().activa("camiones").mapa());
  }

  private void seguimientoDonante(Context ctx) {
    ctx.render("seguimiento-donante.html.hbs",
        Modelo.de("Seguimiento").seccion("Seguimiento")
            .comoDonante().activa("seguimiento").conMapa().mapa());
  }

  private void seguimientoBeneficiaria(Context ctx) {
    ctx.render("seguimiento-beneficiario.html.hbs",
        Modelo.de("Seguimiento").seccion("Seguimiento")
            .comoBeneficiaria().activa("seguimiento").conMapa().mapa());
  }

  private void confirmarRecepcion(Context ctx) {
    ctx.render("confirmar-recepcion.html.hbs",
        Modelo.de("Confirmar recepción").seccion("Confirmar recepción")
            .comoBeneficiaria().activa("confirmar").mapa());
  }
}
