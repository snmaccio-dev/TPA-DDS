package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;

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