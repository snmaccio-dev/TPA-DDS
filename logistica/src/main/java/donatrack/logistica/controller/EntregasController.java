package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.ConfirmarRecepcionRequest;
import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.controller.dto.NoRecepcionRequest;
import donatrack.logistica.service.GestorEntregas;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class EntregasController {

  private final GestorEntregas gestor;

  public EntregasController(GestorEntregas gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.get("/entregas", this::todas);
    app.get("/entregas/{id}", this::buscar);
    app.post("/entregas/{id}/confirmacion", this::confirmar);
    app.post("/entregas/{id}/no-recepcion", this::noRecibida);
    app.post("/entregas/{id}/retorno-deposito", this::retornarADeposito);
  }

  private void todas(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.todas()));
  }

  private void buscar(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.buscar(idDe(ctx))));
  }

  private void confirmar(Context ctx) {
    ConfirmarRecepcionRequest request = ctx.bodyAsClass(ConfirmarRecepcionRequest.class);
    ctx.json(EntregaDTO.desde(gestor.confirmarRecepcion(idDe(ctx), request.fotosOVacio())));
  }

  private void noRecibida(Context ctx) {
    NoRecepcionRequest request = ctx.bodyAsClass(NoRecepcionRequest.class);
    ctx.json(EntregaDTO.desde(gestor.marcarNoRecibida(idDe(ctx), request.motivo())));
  }

  private void retornarADeposito(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.retornarADeposito(idDe(ctx))));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
