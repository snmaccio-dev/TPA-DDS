package donatrack.logistica.repository;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.entrega.EstadoEntrega;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioEntregas implements WithSimplePersistenceUnit {

  private static RepositorioEntregas instancia;

  private RepositorioEntregas() {
  }

  public static RepositorioEntregas getInstance() {
    if (instancia == null) {
      instancia = new RepositorioEntregas();
    }

    return instancia;
  }

  public void guardar(Entrega entrega) {
    persist(entrega);
  }

  public void guardar(List<Entrega> nuevasEntregas) {
    nuevasEntregas.forEach(this::persist);
  }

  public List<Entrega> todas() {
    return createQuery("select e from Entrega e", Entrega.class).getResultList();
  }

  public Optional<Entrega> buscarPorId(long id) {
    return Optional.ofNullable(find(Entrega.class, id));
  }

  public Optional<Entrega> buscarPorDonacion(long donacionId) {
    return createQuery(
        "select e from Entrega e where e.donacionId = :donacionId",
        Entrega.class)
        .setParameter("donacionId", donacionId)
        .getResultList()
        .stream()
        .findFirst();
  }

  public List<Entrega> queEsperanPlanificacion() {
    return createQuery(
        "select e from Entrega e "
            + "where e.estado = :estado "
            + "and e.pesoKg is not null "
            + "and e.volumenM3 is not null",
        Entrega.class)
        .setParameter("estado", EstadoEntrega.PENDIENTE)
        .getResultList();
  }

  public List<Entrega> pendientesDeMedicion() {
    return createQuery(
        "select e from Entrega e "
            + "where e.estado = :estado "
            + "and e.ruta is null "
            + "and (e.pesoKg is null or e.volumenM3 is null)",
        Entrega.class)
        .setParameter("estado", EstadoEntrega.PENDIENTE)
        .getResultList();
  }

  public void eliminar(long id) {
    buscarPorId(id).ifPresent(this::remove);
  }

  public int cantidad() {
    return createQuery("select count(e) from Entrega e", Long.class)
        .getSingleResult()
        .intValue();
  }
}
