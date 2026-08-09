package donatrack.api;

import donatrack.gestion.GestorCamiones;
import donatrack.model.logistica.Camion;

import java.util.List;

public class CamionesController {

  private final GestorCamiones gestor =
      new GestorCamiones();

  // GET /camiones
  public List<Camion> todas() {
    return gestor.todas();
  }

  // GET /camiones/{patente}
  public Camion buscar(String patente) {
    return gestor.buscar(patente);
  }

  // POST /camiones
  public Camion crear(Camion camion) {
    return gestor.crear(camion);
  }

  // DELETE /camiones/{patente}
  public void eliminar(String patente) {
    gestor.eliminar(patente);
  }
}
