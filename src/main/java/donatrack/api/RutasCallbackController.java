package donatrack.api;

import donatrack.gestion.GestorLogistica;
import donatrack.model.donacion.Donacion;
import donatrack.model.logistica.RutaReparto;

import java.util.List;

public class RutasCallbackController {

  private final GestorLogistica gestorLogistica;

  public RutasCallbackController(GestorLogistica gestorLogistica) {
    this.gestorLogistica = gestorLogistica;
  }

  public void recibirResultado(
      List<RutaReparto> rutas,
      List<Donacion> noAsignadas) {

    gestorLogistica.procesarResultadoPlanificacion(
        rutas,
        noAsignadas
    );
  }
}
