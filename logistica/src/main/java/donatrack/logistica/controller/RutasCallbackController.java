package donatrack.logistica.controller;

import donatrack.logistica.contrato.PlanificacionRecibida;
import donatrack.logistica.controller.dto.RutaDTO;
import donatrack.logistica.service.GestorLogistica;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

public class RutasCallbackController {

  private static final String HEADER_TOKEN = "X-Planificador-Token";

  private final GestorLogistica gestorLogistica;
  private final String tokenEsperado;

  public RutasCallbackController(GestorLogistica gestorLogistica, String tokenEsperado) {
    this.gestorLogistica = gestorLogistica;
    this.tokenEsperado = tokenEsperado;
  }

  public void registrarRutas(Javalin app) {
    app.post("/planificacion/callback", this::recibirResultado);
  }

  private void recibirResultado(Context ctx) {
    exigirToken(ctx);

    PlanificacionRecibida recibida = ctx.bodyAsClass(PlanificacionRecibida.class);

    ctx.json(RutaDTO.desde(
        gestorLogistica.procesarCallbackDelProveedor(recibida).getRutas()
    ));
  }

  private void exigirToken(Context ctx) {
    if (!tokenEsperado.equals(ctx.header(HEADER_TOKEN))) {
      throw new UnauthorizedResponse("Token del planificador invalido o ausente.");
    }
  }
}
