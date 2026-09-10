package donatrack.logistica.service;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioRutas;

import java.util.List;

public class GestorRutas {

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
            new IllegalArgumentException(
                "No existe una ruta con ID " + id
            ));
  }

  // POST /rutas
  public RutaReparto crear(RutaReparto ruta) {
    repositorio.guardar(ruta);
    return ruta;
  }

  // DELETE /rutas/{id}
  public void eliminar(long id) {
    repositorio.eliminar(id);
  }
}