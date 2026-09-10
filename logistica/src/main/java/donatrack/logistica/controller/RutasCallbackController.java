package donatrack.logistica.controller;

import donatrack.logistica.service.GestorLogistica;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.logistica.domain.ruta.RutaReparto;

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
