package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.controller.dto.IniciarRutaRequest;
import donatrack.logistica.controller.dto.RutaDTO;
import donatrack.logistica.service.GestorEntregas;
import donatrack.logistica.service.GestorRutas;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class RutasController {

  private final GestorRutas gestorRutas;
  private final GestorEntregas gestorEntregas;

  public RutasController(GestorRutas gestorRutas, GestorEntregas gestorEntregas) {
    this.gestorRutas = gestorRutas;
    this.gestorEntregas = gestorEntregas;
  }

  public void registrarRutas(Javalin app) {
    app.get("/rutas", this::todas);
    app.get("/rutas/{id}", this::buscar);
    app.get("/rutas/{id}/entregas", this::entregas);
    app.post("/rutas/{id}/inicio", this::iniciar);
    app.delete("/rutas/{id}", this::eliminar);
  }

  private void todas(Context ctx) {
    ctx.json(RutaDTO.desde(gestorRutas.todas()));
  }

  private void buscar(Context ctx) {
    ctx.json(RutaDTO.desde(gestorRutas.buscar(idDe(ctx))));
  }

  private void entregas(Context ctx) {
    ctx.json(EntregaDTO.desde(gestorEntregas.deRuta(idDe(ctx))));
  }

  private void iniciar(Context ctx) {
    IniciarRutaRequest request = ctx.bodyAsClass(IniciarRutaRequest.class);
    ctx.json(RutaDTO.desde(gestorEntregas.iniciarRuta(idDe(ctx), request.aChofer())));
  }

  private void eliminar(Context ctx) {
    gestorRutas.buscar(idDe(ctx));
    gestorRutas.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
