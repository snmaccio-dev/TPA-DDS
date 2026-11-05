package donatrack.web.vista;

public record CamionVista(
    String patente,
    double capacidadVolumen,
    double altura,
    double capacidadCarga,
    String situacion
) {

  private static final String DISPONIBLE = "DISPONIBLE";

  public String getPatente() {
    return patente;
  }

  public String getSituacion() {
    return situacion;
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
    return DISPONIBLE.equals(situacion);
  }

  public String getSituacionTexto() {
    return isDisponible() ? "Disponible" : "En ruta";
  }

  public String getSituacionEstilo() {
    return isDisponible() ? "success" : "info";
  }

  public String getVolumenTexto() {
    return Formato.metrosCubicos(capacidadVolumen);
  }

  public String getAlturaTexto() {
    return Formato.metros(altura);
  }

  public String getCargaTexto() {
    return Formato.kilos(capacidadCarga);
  }
}
