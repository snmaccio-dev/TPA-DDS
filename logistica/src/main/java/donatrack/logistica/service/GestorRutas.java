package donatrack.logistica.service;

import donatrack.logistica.domain.TransicionNoPermitidaException;
import donatrack.logistica.domain.ruta.EstadoRuta;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.ArrayList;
import java.util.List;

public class GestorRutas implements WithSimplePersistenceUnit {

  private final RepositorioRutas repositorio =
      RepositorioRutas.getInstance();

  // GET /rutas
  public List<RutaReparto> todas() {
    return repositorio.todas();
  }

  // GET /rutas/{id}
  public RutaReparto buscar(long id) {
    return repositorio.buscarPorId(id)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe una ruta con ID " + id
            ));
  }

  // GET /rutas/mias
  // Las planificadas estan disponibles para que cualquier chofer las tome; las en curso
  // son solo las que inicio el chofer que consulta.
  public List<RutaReparto> paraElChofer(long choferId) {
    List<RutaReparto> rutas = new ArrayList<>(repositorio.planificadas());
    rutas.addAll(repositorio.enCursoDeChofer(choferId));

    return rutas;
  }

  // POST /rutas
  public RutaReparto crear(RutaReparto ruta) {
    withTransaction(() -> repositorio.guardar(ruta));
    return ruta;
  }

  // DELETE /rutas/{id}
  public void eliminar(long id) {
    withTransaction(() -> {
      RutaReparto ruta = buscar(id);

      if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
        throw new TransicionNoPermitidaException(
            "La ruta " + id + " ya fue iniciada: no se puede eliminar."
        );
      }

      ruta.liberarEntregas();
      repositorio.eliminar(id);
    });
  }
}
