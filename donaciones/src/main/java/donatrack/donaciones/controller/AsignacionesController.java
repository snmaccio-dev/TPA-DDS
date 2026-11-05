package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.BeneficiariaDTO;
import donatrack.donaciones.controller.dto.ConfirmarDestinatarioRequest;
import donatrack.donaciones.controller.dto.PropuestaDTO;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.service.GestorAsignaciones;
import donatrack.donaciones.service.RecursoInexistenteException;
import donatrack.donaciones.service.ResultadoCorrida;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class AsignacionesController {

  private final GestorAsignaciones gestor;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioEntidades repositorioEntidades;

  public AsignacionesController(GestorAsignaciones gestor,
                                RepositorioDonaciones repositorioDonaciones,
                                RepositorioEntidades repositorioEntidades) {
    this.gestor = gestor;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioEntidades = repositorioEntidades;
  }

  public void registrarRutas(Javalin app) {
    app.get("/asignaciones/propuestas", this::propuestas, RolDeAcceso.ADMINISTRADOR);
    app.post("/asignaciones/ejecutar", this::ejecutarMatchmaking, RolDeAcceso.ADMINISTRADOR);
    app.get("/donaciones/{id}/propuesta", this::propuestaDeDonacion, RolDeAcceso.ADMINISTRADOR);
    app.post("/donaciones/{id}/asignacion", this::ejecutarAsignacion, RolDeAcceso.ADMINISTRADOR);
    app.post("/donaciones/{id}/destinatario", this::confirmarDestinatario, RolDeAcceso.ADMINISTRADOR);
  }

  private void propuestas(Context ctx) {
    ctx.json(PropuestaDTO.desde(gestor.propuestas()));
  }

  private void ejecutarMatchmaking(Context ctx) {
    ResultadoCorrida resultado = gestor.ejecutarMatchmakingProgramado();
    ctx.json(new EjecutarMatchmakingResponse(resultado.exitosas(), resultado.fallidas()));
  }

  private void propuestaDeDonacion(Context ctx) {
    long donacionId = idDe(ctx);
    PropuestaAsignacion propuesta = gestor.buscarPropuesta(donacionId)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No hay propuesta generada para la donación " + donacionId
        ));
    ctx.json(PropuestaDTO.desde(propuesta));
  }

  private void ejecutarAsignacion(Context ctx) {
    Donacion donacion = buscarDonacion(idDe(ctx));
    var candidatas = gestor.ejecutarAsignacion(donacion);
    ctx.json(BeneficiariaDTO.desde(candidatas));
  }

  private void confirmarDestinatario(Context ctx) {
    ConfirmarDestinatarioRequest request = ctx.bodyAsClass(ConfirmarDestinatarioRequest.class);
    Beneficiaria destinatario = repositorioEntidades.buscarPorId(request.beneficiariaId())
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la beneficiaria con id " + request.beneficiariaId()
        ));
    gestor.confirmarDestino(idDe(ctx), destinatario);
    ctx.status(204);
  }

  private Donacion buscarDonacion(long donacionId) {
    return repositorioDonaciones.buscarPorId(donacionId)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la donación con ID " + donacionId
        ));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }

  public record EjecutarMatchmakingResponse(int procesadas, List<String> fallidas) {}
}