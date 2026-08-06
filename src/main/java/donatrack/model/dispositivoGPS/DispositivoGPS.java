package donatrack.model.dispositivoGPS;

import donatrack.model.logistica.Camion;

public class DispositivoGPS implements ProveedorUbicacion {

  @Override
  public EstadoRecorrido obtenerEstado(Camion camion) {

    // Simulación de datos enviados por el dispositivo GPS externo
    Posicion posicion = new Posicion(
        -34.6037,  // latitud ejemplo CABA
        -58.3816,  // longitud ejemplo CABA
        45.0       // velocidad km/h
    );

    double avance = 65.0;

    validarDatos(posicion, avance);

    return new EstadoRecorrido(posicion, avance);
  }


  private void validarDatos(Posicion posicion, double avance) {

    if (posicion == null) {
      throw new IllegalArgumentException(
          "El GPS no envió una posición válida."
      );
    }

    if (avance < 0 || avance > 100) {
      throw new IllegalArgumentException(
          "Porcentaje de avance inválido."
      );
    }
  }
}

