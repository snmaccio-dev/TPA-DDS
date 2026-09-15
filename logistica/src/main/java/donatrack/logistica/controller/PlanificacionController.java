package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.CorridaDTO;
import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.service.GestorLogistica;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class PlanificacionController {

  private final GestorLogistica gestor;

  public PlanificacionController(GestorLogistica gestor) {
    this.gestor = gestor;
  }

  public void registrarRutas(Javalin app) {
    app.post("/planificacion/corridas", this::ejecutarCorrida);
    app.get("/planificacion/pendientes", this::pendientes);
    app.get("/planificacion/demoradas", this::demoradas);
  }

  private void ejecutarCorrida(Context ctx) {
    List<String> solicitudes = gestor.ejecutarCorrida();

    ctx.status(202).json(new CorridaDTO(
        solicitudes,
        gestor.entregasPendientes().size()
    ));
  }

  private void pendientes(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.entregasPendientes()));
  }

  private void demoradas(Context ctx) {
    ctx.json(EntregaDTO.desde(gestor.entregasQueSuperaronLosIntentos()));
  }
}
