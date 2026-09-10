package donatrack.donaciones.controller;

import donatrack.donaciones.service.GestorNecesidades;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.necesidad.Necesidad;

import java.util.List;

public class NecesidadesController {

  private final GestorNecesidades gestor =
      new GestorNecesidades();

  // GET /necesidades
  public List<Necesidad> todas() {
    return gestor.todas();
  }

  // GET /necesidades/{id}
  public Necesidad buscar(long id) {
    return gestor.buscar(id);
  }

  // POST /necesidades
  public Necesidad crear(Necesidad necesidad) {
    return gestor.crear(necesidad);
  }

  // PUT /necesidades/{id}
  public void actualizar(
      long id,
      String descripcion,
      int cantidad,
      Subcategoria subcategoria
  ) {
    gestor.actualizar(
        id,
        descripcion,
        cantidad,
        subcategoria
    );
  }

  // DELETE /necesidades/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }
}
