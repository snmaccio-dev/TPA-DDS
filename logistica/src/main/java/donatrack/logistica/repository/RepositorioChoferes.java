package donatrack.logistica.repository;

import donatrack.logistica.domain.flota.Chofer;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioChoferes implements WithSimplePersistenceUnit {

  private static RepositorioChoferes instancia;

  private RepositorioChoferes() {
  }

  public static RepositorioChoferes getInstance() {
    if (instancia == null) {
      instancia = new RepositorioChoferes();
    }

    return instancia;
  }

  public void guardar(Chofer chofer) {
    persist(chofer);
  }

  public List<Chofer> todos() {
    return createQuery("select c from Chofer c", Chofer.class).getResultList();
  }

  public Optional<Chofer> buscarPorId(long id) {
    return Optional.ofNullable(find(Chofer.class, id));
  }

  public Optional<Chofer> buscarPorDocumento(String documento) {
    return createQuery(
        "select c from Chofer c where c.documento = :documento",
        Chofer.class)
        .setParameter("documento", documento)
        .getResultList()
        .stream()
        .findFirst();
  }

  public void eliminar(long id) {
    buscarPorId(id).ifPresent(this::remove);
  }
}
