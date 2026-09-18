package donatrack.logistica.service;

import donatrack.logistica.domain.ruta.EstadoRuta;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

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

  // POST /rutas
  public RutaReparto crear(RutaReparto ruta) {
    withTransaction(() -> repositorio.guardar(ruta));
    return ruta;
  }

  // DELETE /rutas/{id}
  // Cambio disparado por las FKs que introdujo la persistencia: el borrado directo dejaba
  // entregas apuntando a destinos ya eliminados. Ahora solo se borra una ruta no iniciada,
  // y antes se liberan sus entregas. Version anterior:
  // public void eliminar(long id) {
  //   withTransaction(() -> repositorio.eliminar(id));
  // }
  public void eliminar(long id) {
    withTransaction(() -> {
      RutaReparto ruta = buscar(id);

      if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
        throw new IllegalStateException(
            "La ruta " + id + " ya fue iniciada: no se puede eliminar."
        );
      }

      ruta.liberarEntregas();
      repositorio.eliminar(id);
    });
  }
}
