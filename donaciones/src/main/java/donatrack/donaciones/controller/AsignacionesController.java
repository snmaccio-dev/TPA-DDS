package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.BeneficiariaDTO;
import donatrack.donaciones.controller.dto.ConfirmarDestinatarioRequest;
import donatrack.donaciones.controller.dto.PropuestaDTO;
import donatrack.donaciones.service.AsignacionesService;
import donatrack.donaciones.service.ResultadoCorrida;
import io.javalin.http.Context;

import java.util.List;

public class AsignacionesController {

  private final AsignacionesService service;

  public AsignacionesController(AsignacionesService service) {
    this.service = service;
  }

  public void propuestas(Context ctx) {
    ctx.json(PropuestaDTO.desde(service.propuestas()));
  }

  public void ejecutarMatchmaking(Context ctx) {
    ResultadoCorrida resultado = service.ejecutarMatchmakingProgramado();
    ctx.json(new EjecutarMatchmakingResponse(resultado.exitosas(), resultado.fallidas()));
  }

  public void propuestaDeDonacion(Context ctx) {
    ctx.json(PropuestaDTO.desde(service.propuestaDe(idDe(ctx))));
  }

  public void ejecutarAsignacion(Context ctx) {
    ctx.json(BeneficiariaDTO.desde(service.ejecutarAsignacion(idDe(ctx))));
  }

  public void confirmarDestinatario(Context ctx) {
    ConfirmarDestinatarioRequest request = ctx.bodyAsClass(ConfirmarDestinatarioRequest.class);
    service.confirmarDestino(idDe(ctx), request.beneficiariaId());
    ctx.status(204);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }

  public record EjecutarMatchmakingResponse(int procesadas, List<String> fallidas) {}
}
