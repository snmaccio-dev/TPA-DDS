package donatrack.repositorio;

import donatrack.model.necesidad.Necesidad;

import java.util.*;

public class RepositorioNecesidades {

  private static RepositorioNecesidades instancia;

  private final Map<Long, Necesidad> necesidades =
      new HashMap<>();

  private RepositorioNecesidades() {
  }

  public static RepositorioNecesidades getInstance() {
    if (instancia == null) {
      instancia = new RepositorioNecesidades();
    }

    return instancia;
  }

  public void guardar(Necesidad necesidad) {
    necesidades.put(
        necesidad.getId(),
        necesidad
    );
  }

  public Optional<Necesidad> buscarPorId(long id) {
    return Optional.ofNullable(
        necesidades.get(id)
    );
  }

  public List<Necesidad> todas() {
    return new ArrayList<>(
        necesidades.values()
    );
  }

  public void eliminar(long id) {
    necesidades.remove(id);
  }
}