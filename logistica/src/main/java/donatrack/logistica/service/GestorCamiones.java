package donatrack.logistica.service;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.repository.RepositorioCamiones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;

public class GestorCamiones implements WithSimplePersistenceUnit {

  private final RepositorioCamiones repositorio =
      RepositorioCamiones.getInstance();

  public List<Camion> todas() {
    return repositorio.todas();
  }

  public Camion buscar(String patente) {
    return repositorio.buscar(patente)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe el camión con patente " + patente
            ));
  }

  public Camion crear(Camion camion) {
    withTransaction(() -> repositorio.guardar(camion));
    return camion;
  }

  public void eliminar(String patente) {
    withTransaction(() -> repositorio.eliminar(patente));
  }
}
