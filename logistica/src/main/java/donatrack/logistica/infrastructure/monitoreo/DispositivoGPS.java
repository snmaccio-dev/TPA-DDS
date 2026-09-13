package donatrack.logistica.infrastructure.monitoreo;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.monitoreo.EstadoRecorrido;
import donatrack.logistica.domain.monitoreo.Posicion;
import donatrack.logistica.domain.monitoreo.ProveedorUbicacion;

public class DispositivoGPS implements ProveedorUbicacion {

  @Override
  public EstadoRecorrido obtenerEstado(Camion camion) {

    // Datos enviados por el GPS externo
    Posicion posicion = new Posicion(
        -34.6037,
        -58.3816
    );

    double velocidad = 45.0; // km/h
    double avance = 65.0; // porcentaje

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
          "El GPS no envió una posición válida."
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

