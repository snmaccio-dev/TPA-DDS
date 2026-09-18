package donatrack.logistica.repository;

import donatrack.logistica.domain.flota.Camion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioCamiones implements WithSimplePersistenceUnit {

  private static RepositorioCamiones instancia;

  private RepositorioCamiones() {}

  public static RepositorioCamiones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioCamiones();
    }
    return instancia;
  }

  public void guardar(Camion camion) {
    persist(camion);
  }

  public Optional<Camion> buscar(String patente) {
    return createQuery(
        "select c from Camion c where c.patente = :patente",
        Camion.class)
        .setParameter("patente", patente)
        .getResultList()
        .stream()
        .findFirst();
  }

  public List<Camion> todas() {
    return createQuery("select c from Camion c", Camion.class).getResultList();
  }

  public void eliminar(String patente) {
    buscar(patente).ifPresent(this::remove);
  }
}
