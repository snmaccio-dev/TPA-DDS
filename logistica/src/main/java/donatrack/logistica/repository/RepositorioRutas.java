package donatrack.logistica.repository;

import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioRutas implements WithSimplePersistenceUnit {

  private static RepositorioRutas instancia;

  private RepositorioRutas() {
  }

  public static RepositorioRutas getInstance() {
    if (instancia == null) {
      instancia = new RepositorioRutas();
    }

    return instancia;
  }

  public void guardar(RutaReparto ruta) {
    persist(ruta);
  }

  public void guardar(List<RutaReparto> nuevasRutas) {
    nuevasRutas.forEach(this::persist);
  }

  public List<RutaReparto> todas() {
    return createQuery("select r from RutaReparto r", RutaReparto.class).getResultList();
  }

  public Optional<RutaReparto> buscarPorId(long id) {
    return Optional.ofNullable(find(RutaReparto.class, id));
  }

  public void eliminar(long id) {
    buscarPorId(id).ifPresent(this::remove);
  }
}
