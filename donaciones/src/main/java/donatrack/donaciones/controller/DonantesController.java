package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.DonanteInactivoDTO;
import donatrack.donaciones.controller.dto.NuevoDonanteRequest;
import donatrack.donaciones.controller.dto.PersonaDTO;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.PersonasService;
import donatrack.donaciones.service.ResultadoCorrida;
import io.javalin.http.Context;

import java.util.List;

public class DonantesController {

  private final PersonasService gestor;
  private final DonantesService gestorDonantes;

  public DonantesController(PersonasService gestor, DonantesService gestorDonantes) {
    this.gestor = gestor;
    this.gestorDonantes = gestorDonantes;
  }

  public void todas(Context ctx) {
    ctx.json(PersonaDTO.desde(gestor.todos()));
  }

  public void buscar(Context ctx) {
    ctx.json(PersonaDTO.desde(gestor.buscarPorDocumento(ctx.pathParam("documento"))));
  }

  public void inactivos(Context ctx) {
    ctx.json(DonanteInactivoDTO.desde(gestorDonantes.inactivos()));
  }

  public void avisarInactivos(Context ctx) {
    ResultadoCorrida resultado = gestorDonantes.avisarInactivos();
    ctx.json(new AvisarInactivosResponse(resultado.exitosas(), resultado.fallidas()));
  }

  public void registrar(Context ctx) {
    NuevoDonanteRequest request = ctx.bodyAsClass(NuevoDonanteRequest.class);
    Persona persona = request.aPersona();
    if (request.direccion() != null && !request.direccion().isBlank()) {
      persona.setDireccion(request.direccion());
    }
    gestor.registrar(persona, request.email());
    ctx.status(201);
  }

  public void eliminar(Context ctx) {
    gestor.eliminar(ctx.pathParam("documento"));
    ctx.status(204);
  }

  public record AvisarInactivosResponse(int avisados, List<String> fallidos) {}
}