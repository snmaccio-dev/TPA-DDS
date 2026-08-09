package donatrack.model.planificacion;

import donatrack.model.donacion.Donacion;
import donatrack.model.logistica.RutaReparto;

import java.util.List;

public class ResultadoPlanificacion {

  private final List<RutaReparto> rutas;
  private final List<Donacion> donacionesNoAsignadas;

  public ResultadoPlanificacion(
      List<RutaReparto> rutas,
      List<Donacion> donacionesNoAsignadas) {

    this.rutas = rutas;
    this.donacionesNoAsignadas =
        donacionesNoAsignadas;
  }

  public List<RutaReparto> getRutas() {
    return rutas;
  }

  public List<Donacion> getDonacionesNoAsignadas() {
    return donacionesNoAsignadas;
  }
}