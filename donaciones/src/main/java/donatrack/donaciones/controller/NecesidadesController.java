package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ActualizarNecesidadRequest;
import donatrack.donaciones.controller.dto.NecesidadDTO;
import donatrack.donaciones.controller.dto.NuevaNecesidadRequest;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import donatrack.donaciones.service.NecesidadesService;
import io.javalin.http.Context;

public class NecesidadesController {

  private final NecesidadesService service;
  private final EntidadesBeneficiariasService entidadesService;

  public NecesidadesController(NecesidadesService service,
                               EntidadesBeneficiariasService entidadesService) {
    this.service = service;
    this.entidadesService = entidadesService;
  }

  public void todas(Context ctx) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
    if (usuario.esAdministrador()) {
      ctx.json(NecesidadDTO.desde(service.todas()));
    } else {
      Beneficiaria propia = Acceso.perfilBeneficiariaObligatorio(usuario);
      ctx.json(NecesidadDTO.desde(service.deBeneficiaria(propia)));
    }
  }

  public void buscar(Context ctx) {
    Necesidad necesidad = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    ctx.json(NecesidadDTO.desde(necesidad));
  }

  public void crear(Context ctx) {
    NuevaNecesidadRequest request = ctx.bodyAsClass(NuevaNecesidadRequest.class);
    Beneficiaria beneficiaria = beneficiariaPara(FiltroDeAcceso.usuarioDe(ctx), request.cuitBeneficiaria());
    Necesidad necesidad = service.crear(
        beneficiaria,
        request.tipo(),
        request.descripcion(),
        request.cantidad(),
        request.subcategoria(),
        request.periodo()
    );
    ctx.status(201).json(NecesidadDTO.desde(necesidad));
  }

  public void actualizar(Context ctx) {
    Necesidad necesidad = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    ActualizarNecesidadRequest request = ctx.bodyAsClass(ActualizarNecesidadRequest.class);
    service.actualizar(
        necesidad.getId(),
        request.descripcion(),
        request.cantidad(),
        request.subcategoria()
    );
    ctx.status(204);
  }

  public void eliminar(Context ctx) {
    Necesidad necesidad = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    service.eliminar(necesidad.getId());
    ctx.status(204);
  }

  // Para BENEFICIARIA ignora el cuit del body y usa siempre la propia; para
  // ADMINISTRADOR se respeta el cuit indicado.
  private Beneficiaria beneficiariaPara(UsuarioActual usuario, String cuitDelBody) {
    if (usuario.esBeneficiaria()) {
      return Acceso.perfilBeneficiariaObligatorio(usuario);
    }
    return entidadesService.buscarPorCuit(cuitDelBody);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
