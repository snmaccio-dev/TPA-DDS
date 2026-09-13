package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.CrearCamionRequest;
import donatrack.logistica.service.GestorCamiones;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class CamionesController {

  private final GestorCamiones gestor;

  public CamionesController(GestorCamiones gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.get("/camiones", this::todos);
    app.get("/camiones/{patente}", this::buscar);
    app.post("/camiones", this::crear);
    app.delete("/camiones/{patente}", this::eliminar);
  }

  private void todos(Context ctx) {
    ctx.json(gestor.todas());
  }

  private void buscar(Context ctx) {
    ctx.json(gestor.buscar(ctx.pathParam("patente")));
  }

  private void crear(Context ctx) {
    CrearCamionRequest request = ctx.bodyAsClass(CrearCamionRequest.class);
    ctx.status(201).json(gestor.crear(request.aCamion()));
  }

  private void eliminar(Context ctx) {
    gestor.buscar(ctx.pathParam("patente"));
    gestor.eliminar(ctx.pathParam("patente"));
    ctx.status(204);
  }
}
