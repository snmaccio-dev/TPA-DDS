package donatrack.web.controller;

import donatrack.web.Modelo;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class PublicoController {

  public void registrarRutas(Javalin app) {
    app.get("/", this::landing);
    app.get("/donaciones-publicas", this::donacionesPublicas);
    app.get("/registro", this::registro);
    app.get("/login", this::login);
    app.get("/privacidad", this::privacidad);
  }

  private void landing(Context ctx) {
    ctx.render("index.html.hbs", Modelo.de("Inicio").mapa());
  }

  private void donacionesPublicas(Context ctx) {
    ctx.render("donaciones-publicas.html.hbs",
        Modelo.de("Donaciones entregadas").conMapa().mapa());
  }

  private void registro(Context ctx) {
    ctx.render("registro.html.hbs", Modelo.de("Crear cuenta").mapa());
  }

  private void login(Context ctx) {
    ctx.render("login.html.hbs", Modelo.de("Iniciar sesión").mapa());
  }

  private void privacidad(Context ctx) {
    ctx.render("privacidad.html.hbs", Modelo.de("Privacidad").mapa());
  }
}
