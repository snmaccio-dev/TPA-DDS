package donatrack.logistica.domain.planificacion;

import donatrack.donaciones.domain.donacion.Donacion;

import java.util.List;

public interface GeneradorRutas {

  List<RutaReparto> generar(
      List<Donacion> donaciones,
      List<Camion> camiones
  );
}