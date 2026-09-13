package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.BeneficiariaDTO;
import donatrack.donaciones.controller.dto.ConfirmarDestinatarioRequest;
import donatrack.donaciones.controller.dto.PropuestaDTO;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Administrador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.service.GestorAsignaciones;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class AsignacionesController {

  private final GestorAsignaciones gestor;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioEntidades repositorioEntidades;
  private final RepositorioPersonas repositorioPersonas;

  public AsignacionesController(GestorAsignaciones gestor,
                                RepositorioDonaciones repositorioDonaciones,
                                RepositorioEntidades repositorioEntidades,
                                RepositorioPersonas repositorioPersonas) {
    this.gestor = gestor;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioEntidades = repositorioEntidades;
    this.repositorioPersonas = repositorioPersonas;
  }

  public void registrarRutas(Javalin app) {
    app.get("/asignaciones/propuestas", this::propuestas);
    app.post("/asignaciones/ejecutar", this::ejecutarMatchmaking);
    app.get("/donaciones/{id}/propuesta", this::propuestaDeDonacion);
    app.post("/donaciones/{id}/asignacion", this::ejecutarAsignacion);
    app.post("/donaciones/{id}/destinatario", this::confirmarDestinatario);
  }

  private void propuestas(Context ctx) {
    ctx.json(PropuestaDTO.desde(gestor.propuestas()));
  }

  private void ejecutarMatchmaking(Context ctx) {
    int procesadas = gestor.ejecutarMatchmakingProgramado();
    ctx.json(new EjecutarMatchmakingResponse(procesadas));
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
    Administrador administrador = administradorDe(request.administradorDocumento());
    gestor.confirmarDestino(idDe(ctx), destinatario, administrador);
    ctx.status(204);
  }

  private Donacion buscarDonacion(long donacionId) {
    return repositorioDonaciones.buscarPorId(donacionId)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la donación con ID " + donacionId
        ));
  }

  private Administrador administradorDe(String documento) {
    Persona persona = repositorioPersonas.buscarPorDocumento(documento)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la persona con documento " + documento
        ));
    return persona.comoRol(Administrador.class)
        .orElseThrow(() -> new IllegalArgumentException(
            "La persona con documento " + documento + " no tiene el rol Administrador."
        ));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }

  public record EjecutarMatchmakingResponse(int procesadas) {}
}