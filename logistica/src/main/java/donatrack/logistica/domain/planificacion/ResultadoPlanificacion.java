package donatrack.logistica.domain.planificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.logistica.domain.ruta.RutaReparto;

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