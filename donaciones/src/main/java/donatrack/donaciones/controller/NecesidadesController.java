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
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.UsuarioActual;
import donatrack.donaciones.service.GestorNecesidades;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class NecesidadesController {

  private final GestorNecesidades gestor;
  private final RepositorioSubcategorias repositorioSubcategorias;
  private final RepositorioEntidades repositorioEntidades;

  public NecesidadesController(GestorNecesidades gestor,
                               RepositorioSubcategorias repositorioSubcategorias,
                               RepositorioEntidades repositorioEntidades) {
    this.gestor = gestor;
    this.repositorioSubcategorias = repositorioSubcategorias;
    this.repositorioEntidades = repositorioEntidades;
  }

  public void registrarRutas(Javalin app) {
    app.get("/necesidades", this::todas, RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA);
    app.post("/necesidades", this::crear, RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA);
    app.get("/necesidades/{id}", this::buscar, RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA);
    app.put("/necesidades/{id}", this::actualizar, RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA);
    app.delete("/necesidades/{id}", this::eliminar, RolDeAcceso.ADMINISTRADOR, RolDeAcceso.BENEFICIARIA);
  }

  private void todas(Context ctx) {
    UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
    if (usuario.esAdministrador()) {
      ctx.json(NecesidadDTO.desde(gestor.todas()));
    } else {
      Beneficiaria propia = Acceso.perfilBeneficiariaObligatorio(usuario);
      ctx.json(NecesidadDTO.desde(gestor.deBeneficiaria(propia)));
    }
  }

  private void buscar(Context ctx) {
    Necesidad necesidad = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    ctx.json(NecesidadDTO.desde(necesidad));
  }

  private void crear(Context ctx) {
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
    ctx.status(201).json(NecesidadDTO.desde(gestor.crear(necesidad)));
  }

  private void actualizar(Context ctx) {
    Necesidad necesidad = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    ActualizarNecesidadRequest request = ctx.bodyAsClass(ActualizarNecesidadRequest.class);
    gestor.actualizar(
        necesidad.getId(),
        request.descripcion(),
        request.cantidad(),
        subcategoriaDe(request.subcategoria())
    );
    ctx.status(204);
  }

  private void eliminar(Context ctx) {
    Necesidad necesidad = gestor.buscar(idDe(ctx));
    Acceso.exigirPuedeVerNecesidad(FiltroDeAcceso.usuarioDe(ctx), necesidad);
    gestor.eliminar(necesidad.getId());
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