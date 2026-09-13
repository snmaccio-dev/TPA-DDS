package donatrack.logistica.infrastructure.monitoreo;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.monitoreo.EstadoRecorrido;
import donatrack.logistica.domain.monitoreo.Posicion;
import donatrack.logistica.domain.monitoreo.ProveedorUbicacion;

public class AplicacionMovil implements ProveedorUbicacion {

  @Override
  public EstadoRecorrido obtenerEstado(Camion camion) {

    // Datos enviados por la app del conductor
    Posicion posicion = new Posicion(
        -34.6100,
        -58.3900
    );

    double velocidad = 30.0;
    double avance = 40.0;

    validarDatos(posicion, velocidad, avance);

    return new EstadoRecorrido(
        posicion,
        velocidad,
        avance
    );
  }


  private void validarDatos(
      Posicion posicion,
      double velocidad,
      double avance
  ) {

    if (posicion == null) {
      throw new IllegalArgumentException(
          "La aplicación no envió una posición válida."
      );
    }

    if (velocidad < 0) {
      throw new IllegalArgumentException(
          "Velocidad inválida."
      );
    }

    if (avance < 0 || avance > 100) {
      throw new IllegalArgumentException(
          "Porcentaje de avance inválido."
      );
    }
  }
}

