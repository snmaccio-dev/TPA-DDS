package donatrack.donaciones.service;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.repository.RepositorioNecesidades;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;

public class GestorNecesidades implements WithSimplePersistenceUnit {

  private final RepositorioNecesidades repositorio =
      RepositorioNecesidades.getInstance();

  // POST /necesidades
  public Necesidad crear(Necesidad necesidad) {
    withTransaction(() -> repositorio.guardar(necesidad));
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
            new RecursoInexistenteException(
                "No existe la necesidad con ID "
                    + id
            ));
  }

  // DELETE /necesidades/{id}
  public void eliminar(long id) {
    buscar(id);
    withTransaction(() -> repositorio.eliminar(id));
  }

  public void actualizar(
      long id,
      String descripcion,
      int cantidad,
      Subcategoria subcategoria
  ) {
    withTransaction(() -> {
      Necesidad necesidad = buscar(id);

      necesidad.setDescripcion(descripcion);
      necesidad.setCantidad(cantidad);
      necesidad.setSubcategoria(subcategoria);
    });
  }
}
