package donatrack.logistica.repository;

import donatrack.logistica.domain.entrega.Entrega;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Singleton — unica instancia de almacen de entregas en memoria
public class RepositorioEntregas {

  private static RepositorioEntregas instancia;

  private final List<Entrega> entregas = new ArrayList<>();

  private RepositorioEntregas() {
  }

  public static RepositorioEntregas getInstance() {
    if (instancia == null) {
      instancia = new RepositorioEntregas();
    }

    return instancia;
  }

  public void guardar(Entrega entrega) {
    entregas.add(entrega);
  }

  public void guardar(List<Entrega> nuevasEntregas) {
    entregas.addAll(nuevasEntregas);
  }

  public List<Entrega> todas() {
    return new ArrayList<>(entregas);
  }

  public Optional<Entrega> buscarPorId(long id) {
    return entregas.stream()
        .filter(entrega -> entrega.getId() == id)
        .findFirst();
  }

  public Optional<Entrega> buscarPorDonacion(long donacionId) {
    return entregas.stream()
        .filter(entrega -> entrega.getDonacionId() == donacionId)
        .findFirst();
  }

  public List<Entrega> queEsperanPlanificacion() {
    return entregas.stream()
        .filter(Entrega::esperaPlanificacion)
        .toList();
  }

  public void eliminar(long id) {
    entregas.removeIf(entrega -> entrega.getId() == id);
  }

  public int cantidad() {
    return entregas.size();
  }
}
