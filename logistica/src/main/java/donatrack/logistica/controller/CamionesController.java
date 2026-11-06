package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.ActualizarCamionRequest;
import donatrack.logistica.controller.dto.CamionDTO;
import donatrack.logistica.controller.dto.CrearCamionRequest;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.service.GestorCamiones;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class CamionesController {

  private final GestorCamiones gestor;

  public CamionesController(GestorCamiones gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.get("/camiones", this::todos, RolDeAcceso.ADMINISTRADOR);
    app.get("/camiones/{patente}", this::buscar, RolDeAcceso.ADMINISTRADOR);
    app.post("/camiones", this::crear, RolDeAcceso.ADMINISTRADOR);
    app.put("/camiones/{patente}", this::actualizar, RolDeAcceso.ADMINISTRADOR);
    app.delete("/camiones/{patente}", this::eliminar, RolDeAcceso.ADMINISTRADOR);
  }

  private void todos(Context ctx) {
    ctx.json(CamionDTO.desde(gestor.todas(), gestor.patentesEnRuta()));
  }

  private void buscar(Context ctx) {
    String patente = ctx.pathParam("patente");
    ctx.json(CamionDTO.desde(
        gestor.buscar(patente),
        gestor.patentesEnRuta().contains(patente)
    ));
  }

  private void crear(Context ctx) {
    CrearCamionRequest request = ctx.bodyAsClass(CrearCamionRequest.class);
    ctx.status(201).json(CamionDTO.disponible(gestor.crear(request.aCamion())));
  }

  private void actualizar(Context ctx) {
    ActualizarCamionRequest request = ctx.bodyAsClass(ActualizarCamionRequest.class);
    ctx.json(CamionDTO.disponible(gestor.actualizar(
        ctx.pathParam("patente"),
        request.capacidadVolumen(),
        request.altura(),
        request.capacidadCarga()
    )));
  }

  private void eliminar(Context ctx) {
    gestor.eliminar(ctx.pathParam("patente"));
    ctx.status(204);
  }
}
