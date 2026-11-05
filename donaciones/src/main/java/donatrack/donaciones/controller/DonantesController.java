package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.DonanteInactivoDTO;
import donatrack.donaciones.controller.dto.NuevoDonanteRequest;
import donatrack.donaciones.controller.dto.PersonaDTO;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.service.GestorDonantes;
import donatrack.donaciones.service.GestorPersonas;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class DonantesController {

  private final GestorPersonas gestor;
  private final GestorDonantes gestorDonantes;

  public DonantesController(GestorPersonas gestor, GestorDonantes gestorDonantes) {
    this.gestor = gestor;
    this.gestorDonantes = gestorDonantes;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donantes", this::todas);
    app.post("/donantes", this::registrar);
    // Rutas mas especificas antes que /donantes/{documento}, para que Javalin no se coma
    // "inactivos" como si fuera un documento.
    app.get("/donantes/inactivos", this::inactivos);
    app.post("/donantes/avisar-inactivos", this::avisarInactivos);
    app.get("/donantes/{documento}", this::buscar);
    app.delete("/donantes/{documento}", this::eliminar);
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
    int avisados = gestorDonantes.avisarInactivos();
    ctx.json(new AvisarInactivosResponse(avisados));
  }

  private void registrar(Context ctx) {
    NuevoDonanteRequest request = ctx.bodyAsClass(NuevoDonanteRequest.class);
    Persona persona = personaDe(request);
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

  private Persona personaDe(NuevoDonanteRequest request) {
    if ("HUMANA".equalsIgnoreCase(request.tipo())) {
      return new PersonaHumana(
          request.nombre(),
          request.apellido(),
          request.edad() == null ? 0 : request.edad(),
          request.documento(),
          request.genero() == null ? null : Genero.valueOf(request.genero())
      );
    }
    if ("JURIDICA".equalsIgnoreCase(request.tipo())) {
      return new PersonaJuridica(
          request.cuit(),
          request.razonSocial(),
          request.tipoOrganizacion() == null ? null : TipoOrganizacion.valueOf(request.tipoOrganizacion()),
          request.rubro()
      );
    }
    throw new IllegalArgumentException(
        "El tipo de persona debe ser HUMANA o JURIDICA (recibido: " + request.tipo() + ")."
    );
  }

  public record AvisarInactivosResponse(int avisados) {}
}