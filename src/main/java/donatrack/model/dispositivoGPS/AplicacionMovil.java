package donatrack.model.dispositivoGPS;

import donatrack.model.logistica.Camion;

public class AplicacionMovil implements ProveedorUbicacion {

  @Override
  public EstadoRecorrido obtenerEstado(Camion camion) {

    // Simulación de datos enviados por la app del chofer
    Posicion posicion = new Posicion(
        -34.6100,
        -58.3900,
        30.0
    );

    double avance = 40.0;

    validarDatos(posicion, avance);

    return new EstadoRecorrido(posicion, avance);
  }


  private void validarDatos(Posicion posicion, double avance) {

    if (posicion == null) {
      throw new IllegalArgumentException(
          "La aplicación no envió una posición válida."
      );
    }

    if (avance < 0 || avance > 100) {
      throw new IllegalArgumentException(
          "Porcentaje de avance inválido."
      );
    }
  }
}

