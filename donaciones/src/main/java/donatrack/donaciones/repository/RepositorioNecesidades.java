package donatrack.donaciones.repository;

import donatrack.donaciones.domain.necesidad.Necesidad;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioNecesidades implements WithSimplePersistenceUnit {

  private static RepositorioNecesidades instancia;

  private RepositorioNecesidades() {
  }

  public static RepositorioNecesidades getInstance() {
    if (instancia == null) {
      instancia = new RepositorioNecesidades();
    }

    return instancia;
  }

  public void guardar(Necesidad necesidad) {
    persist(necesidad);
  }

  public Optional<Necesidad> buscarPorId(long id) {
    return Optional.ofNullable(find(Necesidad.class, id));
  }

  public List<Necesidad> todas() {
    return createQuery("select n from Necesidad n", Necesidad.class).getResultList();
  }

  public void eliminar(long id) {
    buscarPorId(id).ifPresent(this::remove);
  }
}
