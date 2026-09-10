package donatrack.logistica.infrastructure.planificacion;

import donatrack.donaciones.domain.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;

public class GeneradorRutasExterno implements GeneradorRutas {

  @Override
  public List<RutaReparto> generar(
      List<Donacion> donaciones,
      List<Camion> camiones) {

    throw new UnsupportedOperationException(
        "La integración con el generador de rutas externo aún no está implementada."
    );
  }
}