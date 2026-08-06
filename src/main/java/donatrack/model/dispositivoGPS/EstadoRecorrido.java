package donatrack.model.dispositivoGPS;

public class EstadoRecorrido {

  private Posicion posicion;
  private double porcentajeAvance;

  public EstadoRecorrido(Posicion posicion, double porcentajeAvance) {
    this.posicion = posicion;
    this.porcentajeAvance = porcentajeAvance;
  }

  public Posicion getPosicion() {
    return posicion;
  }

  public double getPorcentajeAvance() {
    return porcentajeAvance;
  }
}
