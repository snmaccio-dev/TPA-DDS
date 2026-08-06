package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;

import java.util.List;

public interface GeneradorRutas {

  List<RutaReparto> generar(
      List<Donacion> donaciones,
      List<Camion> camiones
  );
}