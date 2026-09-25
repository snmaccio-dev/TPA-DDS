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
    app.get("/necesidades", this::todas);
    app.post("/necesidades", this::crear);
    app.get("/necesidades/{id}", this::buscar);
    app.put("/necesidades/{id}", this::actualizar);
    app.delete("/necesidades/{id}", this::eliminar);
  }

  private void todas(Context ctx) {
    ctx.json(NecesidadDTO.desde(gestor.todas()));
  }

  private void buscar(Context ctx) {
    ctx.json(NecesidadDTO.desde(gestor.buscar(idDe(ctx))));
  }

  private void crear(Context ctx) {
    NuevaNecesidadRequest request = ctx.bodyAsClass(NuevaNecesidadRequest.class);
    Subcategoria subcategoria = subcategoriaDe(request.subcategoria());
    Beneficiaria beneficiaria = beneficiariaDe(request.cuitBeneficiaria());
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
    ActualizarNecesidadRequest request = ctx.bodyAsClass(ActualizarNecesidadRequest.class);
    gestor.actualizar(
        idDe(ctx),
        request.descripcion(),
        request.cantidad(),
        subcategoriaDe(request.subcategoria())
    );
    ctx.status(204);
  }

  private void eliminar(Context ctx) {
    gestor.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private Subcategoria subcategoriaDe(String nombre) {
    return repositorioSubcategorias.buscarPorNombre(nombre)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la subcategoría '" + nombre + "'."
        ));
  }

  private Beneficiaria beneficiariaDe(String cuit) {
    return repositorioEntidades.buscarPorCuit(cuit)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la entidad beneficiaria con CUIT " + cuit
        ));
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
