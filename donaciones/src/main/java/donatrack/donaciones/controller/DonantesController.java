package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.DonanteExistenteRequest;
import donatrack.donaciones.controller.dto.DonanteInactivoDTO;
import donatrack.donaciones.controller.dto.ModificarDonanteRequest;
import donatrack.donaciones.controller.dto.NuevoDonanteRequest;
import donatrack.donaciones.controller.dto.PersonaDTO;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.ResultadoCorrida;
import io.javalin.http.Context;

import java.util.List;

public class DonantesController {

  private final DonantesService service;

  public DonantesController(DonantesService service) {
    this.service = service;
  }

  public void todas(Context ctx) {
    ctx.json(PersonaDTO.desde(service.todos()));
  }

  public void buscar(Context ctx) {
    ctx.json(PersonaDTO.desde(service.buscarPorDocumento(ctx.pathParam("documento"))));
  }

  public void inactivos(Context ctx) {
    ctx.json(DonanteInactivoDTO.desde(service.inactivos()));
  }

  public void avisarInactivos(Context ctx) {
    ResultadoCorrida resultado = service.avisarInactivos();
    ctx.json(new AvisarInactivosResponse(resultado.exitosas(), resultado.fallidas()));
  }

  public void registrar(Context ctx) {
    NuevoDonanteRequest request = ctx.bodyAsClass(NuevoDonanteRequest.class);
    Persona persona = request.aPersona();
    if (request.direccion() != null && !request.direccion().isBlank()) {
      persona.setDireccion(request.direccion());
    }
    service.registrarConPersonaNueva(persona, request.email());
    ctx.status(201);
  }

  public void registrarExistente(Context ctx) {
    DonanteExistenteRequest request = ctx.bodyAsClass(DonanteExistenteRequest.class);
    service.registrarConPersonaExistente(request.documento(), request.email());
    ctx.status(201);
  }

  public void modificar(Context ctx) {
    ModificarDonanteRequest request = ctx.bodyAsClass(ModificarDonanteRequest.class);
    ctx.json(PersonaDTO.desde(service.modificar(ctx.pathParam("documento"), request.aDatos())));
  }

  public void eliminar(Context ctx) {
    service.eliminar(ctx.pathParam("documento"));
    ctx.status(204);
  }

  public record AvisarInactivosResponse(int avisados, List<String> fallidos) {}
}
