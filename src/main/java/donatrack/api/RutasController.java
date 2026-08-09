package donatrack.api;

import donatrack.gestion.GestorRutas;
import donatrack.model.logistica.RutaReparto;

import java.util.List;

public class RutasController {

  private final GestorRutas gestor =
      new GestorRutas();

  // GET /rutas
  public List<RutaReparto> todas() {
    return gestor.todas();
  }

  // GET /rutas/{id}
  public RutaReparto buscar(long id) {
    return gestor.buscar(id);
  }

  // POST /rutas
  public RutaReparto crear(RutaReparto ruta) {
    return gestor.crear(ruta);
  }

  // DELETE /rutas/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }
}
