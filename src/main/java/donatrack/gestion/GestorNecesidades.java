package donatrack.gestion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.necesidad.Necesidad;
import donatrack.repositorio.RepositorioNecesidades;

import java.util.List;

public class GestorNecesidades {

  private final RepositorioNecesidades repositorio =
      RepositorioNecesidades.getInstance();

  // POST /necesidades
  public Necesidad crear(Necesidad necesidad) {
    repositorio.guardar(necesidad);
    return necesidad;
  }

  // GET /necesidades
  public List<Necesidad> todas() {
    return repositorio.todas();
  }

  // GET /necesidades/{id}
  public Necesidad buscar(long id) {
    return repositorio.buscarPorId(id)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "No existe la necesidad con ID "
                    + id
            ));
  }

  // DELETE /necesidades/{id}
  public void eliminar(long id) {
    buscar(id);
    repositorio.eliminar(id);
  }

  public void actualizar(
      long id,
      String descripcion,
      int cantidad,
      Subcategoria subcategoria
  ) {
    Necesidad necesidad = buscar(id);

    necesidad.setDescripcion(descripcion);
    necesidad.setCantidad(cantidad);
    necesidad.setSubcategoria(subcategoria);
  }
}
