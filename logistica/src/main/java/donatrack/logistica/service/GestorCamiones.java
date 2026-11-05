package donatrack.logistica.service;

import donatrack.logistica.domain.ConflictoDeNegocioException;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.repository.RepositorioCamiones;
import donatrack.logistica.repository.RepositorioRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Set;

public class GestorCamiones implements WithSimplePersistenceUnit {

  private final RepositorioCamiones repositorio =
      RepositorioCamiones.getInstance();
  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();

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

  public Set<String> patentesEnRuta() {
    return repositorioRutas.patentesConRutaEnCurso();
  }

  public Camion crear(Camion camion) {
    withTransaction(() -> repositorio.guardar(camion));
    return camion;
  }

  public Camion actualizar(String patente,
                           double capacidadVolumen,
                           double altura,
                           double capacidadCarga) {
    return withTransaction(() -> {
      Camion camion = buscar(patente);

      if (repositorioRutas.patentesConRutaEnCurso().contains(patente)) {
        throw new ConflictoDeNegocioException(
            "El camión " + patente + " está en una ruta en curso y no puede modificarse."
        );
      }

      camion.actualizarCapacidades(capacidadVolumen, altura, capacidadCarga);

      return camion;
    });
  }

  public void eliminar(String patente) {
    withTransaction(() -> {
      buscar(patente);

      if (repositorioRutas.tieneRutasDeCamion(patente)) {
        throw new ConflictoDeNegocioException(
            "El camión " + patente + " tiene rutas asociadas y no puede eliminarse."
        );
      }

      repositorio.eliminar(patente);
    });
  }
}
