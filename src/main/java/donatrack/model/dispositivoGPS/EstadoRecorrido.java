package donatrack.model.dispositivoGPS;

public class EstadoRecorrido {

  private Posicion posicion;
  private double velocidad;
  private double porcentajeAvance;

  public EstadoRecorrido(
      Posicion posicion,
      double velocidad,
      double porcentajeAvance
  ) {
    this.posicion = posicion;
    this.velocidad = velocidad;
    this.porcentajeAvance = porcentajeAvance;
  }

  public Posicion getPosicion() {
    return posicion;
  }

  public double getVelocidad() {
    return velocidad;
  }

  public double getPorcentajeAvance() {
    return porcentajeAvance;
  }
}
