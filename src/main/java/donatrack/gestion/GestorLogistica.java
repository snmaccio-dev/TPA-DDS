package donatrack.gestion;

import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.estado.AsignacionRealizada;
import donatrack.model.logistica.Camion;
import donatrack.model.logistica.GeneradorRutas;
import donatrack.model.logistica.RutaReparto;

import java.util.List;

public class GestorLogistica {

  private GeneradorRutas generadorRutas;

  public GestorLogistica(GeneradorRutas generadorRutas) {
    this.generadorRutas = generadorRutas;
  }


  public List<RutaReparto> planificarRutas(
      List<Donacion> donaciones,
      List<Camion> camiones) {


    List<Donacion> asignadas = donaciones.stream()
        .filter(d -> d.getEstado() instanceof AsignacionRealizada)
        .toList();


    return generadorRutas.generar(asignadas, camiones);
  }
}