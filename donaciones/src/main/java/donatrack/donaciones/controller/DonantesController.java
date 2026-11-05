package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.DonanteInactivoDTO;
import donatrack.donaciones.controller.dto.NuevoDonanteRequest;
import donatrack.donaciones.controller.dto.PersonaDTO;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.service.GestorDonantes;
import donatrack.donaciones.service.GestorPersonas;
import donatrack.donaciones.service.ResultadoCorrida;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class DonantesController {

  private final GestorPersonas gestor;
  private final GestorDonantes gestorDonantes;

  public DonantesController(GestorPersonas gestor, GestorDonantes gestorDonantes) {
    this.gestor = gestor;
    this.gestorDonantes = gestorDonantes;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donantes", this::todas, RolDeAcceso.ADMINISTRADOR);
    app.post("/donantes", this::registrar, RolDeAcceso.ADMINISTRADOR);
    // Rutas mas especificas antes que /donantes/{documento}, para que Javalin no se coma
    // "inactivos" como si fuera un documento.
    app.get("/donantes/inactivos", this::inactivos, RolDeAcceso.ADMINISTRADOR);
    app.post("/donantes/avisar-inactivos", this::avisarInactivos, RolDeAcceso.ADMINISTRADOR);
    app.get("/donantes/{documento}", this::buscar, RolDeAcceso.ADMINISTRADOR);
    app.delete("/donantes/{documento}", this::eliminar, RolDeAcceso.ADMINISTRADOR);
  }

  private void todas(Context ctx) {
    ctx.json(PersonaDTO.desde(gestor.todos()));
  }

  private void buscar(Context ctx) {
    ctx.json(PersonaDTO.desde(gestor.buscarPorDocumento(ctx.pathParam("documento"))));
  }

  private void inactivos(Context ctx) {
    ctx.json(DonanteInactivoDTO.desde(gestorDonantes.inactivos()));
  }

  private void avisarInactivos(Context ctx) {
    ResultadoCorrida resultado = gestorDonantes.avisarInactivos();
    ctx.json(new AvisarInactivosResponse(resultado.exitosas(), resultado.fallidas()));
  }

  private void registrar(Context ctx) {
    NuevoDonanteRequest request = ctx.bodyAsClass(NuevoDonanteRequest.class);
    Persona persona = request.aPersona();
    if (request.direccion() != null && !request.direccion().isBlank()) {
      persona.setDireccion(request.direccion());
    }
    gestor.registrar(persona, request.email());
    ctx.status(201);
  }

  private void eliminar(Context ctx) {
    gestor.eliminar(ctx.pathParam("documento"));
    ctx.status(204);
  }

  public record AvisarInactivosResponse(int avisados, List<String> fallidos) {}
}