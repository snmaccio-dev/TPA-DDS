package donatrack.repositorio;

import donatrack.model.logistica.Camion;

import java.util.*;

public class RepositorioCamiones {

  private static RepositorioCamiones instancia;

  private final Map<String, Camion> camiones = new HashMap<>();

  private RepositorioCamiones() {}

  public static RepositorioCamiones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioCamiones();
    }
    return instancia;
  }

  public void guardar(Camion camion) {
    camiones.put(camion.getPatente(), camion);
  }

  public Optional<Camion> buscar(String patente) {
    return Optional.ofNullable(camiones.get(patente));
  }

  public List<Camion> todas() {
    return new ArrayList<>(camiones.values());
  }

  public void eliminar(String patente) {
    camiones.remove(patente);
  }
}
