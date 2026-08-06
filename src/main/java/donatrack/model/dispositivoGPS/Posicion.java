package donatrack.model.dispositivoGPS;

public class Posicion {

  private double latitud;
  private double longitud;
  private double velocidad;

  public Posicion(double latitud, double longitud, double velocidad) {
    this.latitud = latitud;
    this.longitud = longitud;
    this.velocidad = velocidad;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }

  public double getVelocidad() {
    return velocidad;
  }
}
