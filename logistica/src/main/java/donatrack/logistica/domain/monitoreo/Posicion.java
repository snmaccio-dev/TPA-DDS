package donatrack.logistica.domain.monitoreo;

public class Posicion {

  private double latitud;
  private double longitud;

  public Posicion(double latitud, double longitud) {
    this.latitud = latitud;
    this.longitud = longitud;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }
}
