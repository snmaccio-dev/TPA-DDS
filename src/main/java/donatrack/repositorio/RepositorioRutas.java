package donatrack.repositorio;

import donatrack.model.logistica.RutaReparto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioRutas {

  private static RepositorioRutas instancia;

  private final List<RutaReparto> rutas = new ArrayList<>();

  private RepositorioRutas() {
  }

  public static RepositorioRutas getInstance() {
    if (instancia == null) {
      instancia = new RepositorioRutas();
    }

    return instancia;
  }

  public void guardar(RutaReparto ruta) {
    rutas.add(ruta);
  }

  public void guardar(List<RutaReparto> nuevasRutas) {
    rutas.addAll(nuevasRutas);
  }

  public List<RutaReparto> todas() {
    return new ArrayList<>(rutas);
  }

  public Optional<RutaReparto> buscarPorId(long id) {
    return rutas.stream()
        .filter(ruta -> ruta.getId() == id)
        .findFirst();
  }

  public void eliminar(long id) {
    rutas.removeIf(ruta -> ruta.getId() == id);
  }
}
