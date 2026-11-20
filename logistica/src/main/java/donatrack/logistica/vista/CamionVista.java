package donatrack.logistica.vista;

import donatrack.logistica.domain.flota.Camion;

import java.util.Locale;

public record CamionVista(
    String patente,
    double capacidadVolumen,
    double altura,
    double capacidadCarga,
    boolean disponible
) {

  private static final Locale LOCAL = new Locale("es", "AR");

  public static CamionVista desde(Camion camion, boolean enRuta) {
    return new CamionVista(
        camion.getPatente(),
        camion.getCapacidadVolumen(),
        camion.getAltura(),
        camion.getCapacidadCarga(),
        !enRuta
    );
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

  public boolean isDisponible() {
    return disponible;
  }

  public String getSituacionTexto() {
    return disponible ? "Disponible" : "En ruta";
  }

  public String getSituacionEstilo() {
    return disponible ? "success" : "info";
  }

  public String getVolumenTexto() {
    return String.format(LOCAL, "%,.1f m³", capacidadVolumen);
  }

  public String getAlturaTexto() {
    return String.format(LOCAL, "%,.2f m", altura);
  }

  public String getCargaTexto() {
    return kilos(capacidadCarga);
  }

  static String kilos(double valor) {
    return String.format(LOCAL, "%,.0f kg", valor);
  }
}
