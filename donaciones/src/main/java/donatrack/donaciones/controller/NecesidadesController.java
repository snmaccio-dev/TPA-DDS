package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ActualizarNecesidadRequest;
import donatrack.donaciones.controller.dto.NecesidadDTO;
import donatrack.donaciones.controller.dto.NuevaNecesidadRequest;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.necesidad.NecesidadRecurrente;
import donatrack.donaciones.domain.necesidad.Periodo;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.seguridad.Acceso;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.NecesidadesService;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.http.Context;

public class NecesidadesController {

  private final NecesidadesService service;
  private final RepositorioSubcategorias repositorioSubcategorias;
  private final RepositorioEntidades repositorioEntidades;

  public NecesidadesController(NecesidadesService service,
                               RepositorioSubcategorias repositorioSubcategorias,
                               RepositorioEntidades repositorioEntidades) {
    this.service = service;
    this.repositorioSubcategorias = repositorioSubcategorias;
    this.repositorioEntidades = repositorioEntidades;
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
    Subcategoria subcategoria = subcategoriaDe(request.subcategoria());
    Beneficiaria beneficiaria = beneficiariaPara(FiltroDeAcceso.usuarioDe(ctx), request.cuitBeneficiaria());
    Necesidad necesidad = "RECURRENTE".equalsIgnoreCase(request.tipo())
        ? new NecesidadRecurrente(
            request.descripcion(),
            request.cantidad(),
            subcategoria,
            beneficiaria,
            request.periodo() == null ? null : Periodo.valueOf(request.periodo()))
        : new NecesidadExtraordinaria(
            request.descripcion(),
            request.cantidad(),
            subcategoria,
            beneficiaria);
    ctx.status(201).json(NecesidadDTO.desde(service.crear(necesidad)));
  }

  public void actualizar(Context ctx) {
    Necesidad necesidad = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    ActualizarNecesidadRequest request = ctx.bodyAsClass(ActualizarNecesidadRequest.class);
    service.actualizar(
        necesidad.getId(),
        request.descripcion(),
        request.cantidad(),
        subcategoriaDe(request.subcategoria())
    );
    ctx.status(204);
  }

  public void eliminar(Context ctx) {
    Necesidad necesidad = service.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    service.eliminar(necesidad.getId());
    ctx.status(204);
  }

  private Subcategoria subcategoriaDe(String nombre) {
    return repositorioSubcategorias.buscarPorNombre(nombre)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la subcategoría '" + nombre + "'."
        ));
  }

  // Para BENEFICIARIA ignora el cuit del body y usa siempre la propia; para
  // ADMINISTRADOR se respeta el cuit indicado.
  private Beneficiaria beneficiariaPara(UsuarioActual usuario, String cuitDelBody) {
    if (usuario.esBeneficiaria()) {
      return Acceso.perfilBeneficiariaObligatorio(usuario);
    }
    return repositorioEntidades.buscarPorCuit(cuitDelBody)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la entidad beneficiaria con CUIT " + cuitDelBody
        ));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}