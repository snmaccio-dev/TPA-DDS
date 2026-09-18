package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.CrearChoferRequest;
import donatrack.logistica.service.GestorChoferes;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class ChoferesController {

  private final GestorChoferes gestor;

  public ChoferesController(GestorChoferes gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.get("/choferes", this::todos);
    app.get("/choferes/{id}", this::buscar);
    app.post("/choferes", this::crear);
    app.delete("/choferes/{id}", this::eliminar);
  }

  private void todos(Context ctx) {
    ctx.json(gestor.todos());
  }

  private void buscar(Context ctx) {
    ctx.json(gestor.buscar(idDe(ctx)));
  }

  private void crear(Context ctx) {
    CrearChoferRequest request = ctx.bodyAsClass(CrearChoferRequest.class);
    ctx.status(201).json(gestor.crear(request.aChofer()));
  }

  private void eliminar(Context ctx) {
    gestor.buscar(idDe(ctx));
    gestor.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
