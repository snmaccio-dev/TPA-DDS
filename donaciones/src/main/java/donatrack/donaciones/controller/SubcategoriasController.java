package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.SubcategoriaDTO;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import io.javalin.http.Context;

public class SubcategoriasController {

  private final RepositorioSubcategorias repositorio;

  public SubcategoriasController(RepositorioSubcategorias repositorio) {
    this.repositorio = repositorio;
  }

  public void todas(Context ctx) {
    ctx.json(SubcategoriaDTO.desde(repositorio.todas()));
  }
}
