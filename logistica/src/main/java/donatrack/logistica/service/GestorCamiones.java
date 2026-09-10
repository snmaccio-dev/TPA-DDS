package donatrack.logistica.service;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.repository.RepositorioCamiones;

import java.util.List;

public class GestorCamiones {

  private final RepositorioCamiones repositorio =
      RepositorioCamiones.getInstance();

  public List<Camion> todas() {
    return repositorio.todas();
  }

  public Camion buscar(String patente) {
    return repositorio.buscar(patente)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "No existe el camión con patente " + patente
            ));
  }

  public Camion crear(Camion camion) {
    repositorio.guardar(camion);
    return camion;
  }

  public void eliminar(String patente) {
    repositorio.eliminar(patente);
  }
}
