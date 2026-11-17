package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ActualizarDireccionRequest;
import donatrack.donaciones.controller.dto.BeneficiariaDTO;
import donatrack.donaciones.controller.dto.NuevaBeneficiariaExistenteRequest;
import donatrack.donaciones.controller.dto.NuevaBeneficiariaNuevaRequest;
import donatrack.donaciones.controller.dto.RepresentanteRequest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.service.DatosEntidad;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import donatrack.donaciones.service.PersonasService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public class EntidadesBeneficiariasController {

  private final EntidadesBeneficiariasService service;
  private final PersonasService personasService;

  public EntidadesBeneficiariasController(EntidadesBeneficiariasService service,
                                          PersonasService personasService) {
    this.service = service;
    this.personasService = personasService;
  }

  public record ConsultaCuit(boolean existePersona,
                             String razonSocial,
                             String direccionActual,
                             String telefonoActual) {}

  public void todas(Context ctx) {
    ctx.json(BeneficiariaDTO.desde(service.todas()));
  }

  public void buscar(Context ctx) {
    ctx.json(BeneficiariaDTO.desde(service.buscar(idDe(ctx))));
  }

  public void consultarPorCuit(Context ctx) {
    String cuit = ctx.queryParam("cuit");
    if (cuit == null || cuit.isBlank()) {
      throw new DatosInvalidosException("Debe indicarse el CUIT en el query param.");
    }
    Optional<PersonaJuridica> encontrada = personasService.buscarJuridicaPorCuit(cuit);
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

  public void altaDePersonaExistente(Context ctx) {
    NuevaBeneficiariaExistenteRequest request =
        ctx.bodyAsClass(NuevaBeneficiariaExistenteRequest.class);
    DatosEntidad datos = new DatosEntidad(
        request.direccion(),
        request.telefono(),
        mapRepresentantes(request.representantes())
    );
    ctx.status(201).json(BeneficiariaDTO.desde(
        service.registrarConPersonaExistente(request.cuit(), datos)
    ));
  }

  public void altaConPersonaNueva(Context ctx) {
    NuevaBeneficiariaNuevaRequest request =
        ctx.bodyAsClass(NuevaBeneficiariaNuevaRequest.class);
    PersonaJuridica juridica = new PersonaJuridica(
        request.cuit(),
        request.razonSocial(),
        request.tipoOrganizacion() == null ? null : TipoOrganizacion.valueOf(request.tipoOrganizacion()),
        request.rubro()
    );
    DatosEntidad datos = new DatosEntidad(
        request.direccion(),
        request.telefono(),
        mapRepresentantes(request.representantes())
    );
    ctx.status(201).json(BeneficiariaDTO.desde(
        service.registrarConPersonaNueva(juridica, datos)
    ));
  }

  public void actualizarDireccion(Context ctx) {
    long id = idDe(ctx);
    Acceso.exigirPuedeModificarEntidad(FiltroDeAcceso.usuarioDe(ctx), id);
    ActualizarDireccionRequest request = ctx.bodyAsClass(ActualizarDireccionRequest.class);
    service.actualizarDireccion(id, request.direccion());
    ctx.status(204);
  }

  private List<Representante> mapRepresentantes(List<RepresentanteRequest> representantes) {
    if (representantes == null) {
      return List.of();
    }
    return representantes.stream().map(RepresentanteRequest::aRepresentante).toList();
  }

  public void eliminar(Context ctx) {
    service.eliminar(idDe(ctx));
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
