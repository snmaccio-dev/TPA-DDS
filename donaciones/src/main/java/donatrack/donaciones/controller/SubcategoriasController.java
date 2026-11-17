package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.SubcategoriaDTO;
import donatrack.donaciones.service.DonacionesService;
import io.javalin.http.Context;

public class SubcategoriasController {

  private final DonacionesService service;

  public SubcategoriasController(DonacionesService service) {
    this.service = service;
  }

  public void todas(Context ctx) {
    ctx.json(SubcategoriaDTO.desde(service.subcategorias()));
  }
}
