package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.BeneficiariaDTO;
import donatrack.donaciones.controller.dto.NuevaBeneficiariaExistenteRequest;
import donatrack.donaciones.controller.dto.NuevaBeneficiariaNuevaRequest;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.service.DatosEntidad;
import donatrack.donaciones.service.GestorEntidadesBeneficiarias;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Optional;

public class EntidadesBeneficiariasController {

  private final GestorEntidadesBeneficiarias gestor;

  public EntidadesBeneficiariasController(GestorEntidadesBeneficiarias gestor) {
    this.gestor = gestor;
  }

  public record ConsultaCuit(boolean existePersona,
                             String razonSocial,
                             String direccionActual,
                             String telefonoActual) {}

  public void registrarRutas(Javalin app) {
    app.get("/entidades", this::todas);
    app.get("/entidades/consulta", this::consultarPorCuit);
    app.get("/entidades/{id}", this::buscar);
    app.post("/entidades/existente", this::altaDePersonaExistente);
    app.post("/entidades/nueva", this::altaConPersonaNueva);
    app.delete("/entidades/{id}", this::eliminar);
  }

  private void todas(Context ctx) {
    ctx.json(BeneficiariaDTO.desde(gestor.todas()));
  }

  private void buscar(Context ctx) {
    ctx.json(BeneficiariaDTO.desde(gestor.buscar(idDe(ctx))));
  }

  private void consultarPorCuit(Context ctx) {
    String cuit = ctx.queryParam("cuit");
    if (cuit == null || cuit.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse el CUIT en el query param.");
    }
    Optional<PersonaJuridica> encontrada = gestor.buscarPorCuit(cuit);
    if (encontrada.isEmpty()) {
      ctx.json(new ConsultaCuit(false, null, null, null));
      return;
    }
    PersonaJuridica juridica = encontrada.get();
    ctx.json(new ConsultaCuit(
        true,
        juridica.getRazonSocial(),
        juridica.getDireccion(),
        telefonoActual(juridica)
    ));
  }

  private void altaDePersonaExistente(Context ctx) {
    NuevaBeneficiariaExistenteRequest request =
        ctx.bodyAsClass(NuevaBeneficiariaExistenteRequest.class);
    DatosEntidad datos = new DatosEntidad(request.direccion(), request.telefono());
    ctx.status(201).json(BeneficiariaDTO.desde(
        gestor.registrarDePersonaExistente(request.cuit(), datos)
    ));
  }

  private void altaConPersonaNueva(Context ctx) {
    NuevaBeneficiariaNuevaRequest request =
        ctx.bodyAsClass(NuevaBeneficiariaNuevaRequest.class);
    PersonaJuridica juridica = new PersonaJuridica(
        request.cuit(),
        request.razonSocial(),
        request.tipoOrganizacion() == null ? null : TipoOrganizacion.valueOf(request.tipoOrganizacion()),
        request.rubro()
    );
    DatosEntidad datos = new DatosEntidad(request.direccion(), request.telefono());
    ctx.status(201).json(BeneficiariaDTO.desde(
        gestor.registrarConPersonaNueva(juridica, datos)
    ));
  }

  private void eliminar(Context ctx) {
    gestor.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private String telefonoActual(PersonaJuridica juridica) {
    return juridica.getContactos().stream()
        .filter(c -> c.getTipo() == TipoContacto.TELEFONO)
        .map(MedioContacto::getValor)
        .findFirst()
        .orElse(null);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}