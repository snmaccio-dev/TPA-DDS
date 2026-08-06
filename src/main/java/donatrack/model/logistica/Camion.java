package donatrack.model.logistica;

public class Camion {

  private String patente;
  private double capacidadVolumen;
  private double altura;
  private double capacidadCarga;

  public Camion(String patente,
                double capacidadVolumen,
                double altura,
                double capacidadCarga) {
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.altura = altura;
    this.capacidadCarga = capacidadCarga;
  }

  public String getPatente() {
    return patente;
  }

  public double getCapacidadVolumen() {
    return capacidadVolumen;
  }

  public double getAltura() {
    return altura;
  }

  public double getCapacidadCarga() {
    return capacidadCarga;
  }
}