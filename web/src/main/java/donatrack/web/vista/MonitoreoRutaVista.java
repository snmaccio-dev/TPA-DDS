package donatrack.web.vista;

public record MonitoreoRutaVista(
    long rutaId,
    String patenteCamion,
    String estadoRuta,
    Double latitud,
    Double longitud,
    Double velocidadKmh,
    double porcentajeAvance,
    String ultimoReporte
) {

  public long getRutaId() {
    return rutaId;
  }

  public String getPatenteCamion() {
    return patenteCamion;
  }

  public boolean isTieneUbicacion() {
    return latitud != null && longitud != null;
  }

  public Double getLatitud() {
    return latitud;
  }

  public Double getLongitud() {
    return longitud;
  }

  public String getEstadoTexto() {
    return switch (estadoRuta == null ? "" : estadoRuta) {
      case "PLANIFICADA" -> "Planificada";
      case "EN_CURSO" -> "En tránsito";
      case "FINALIZADA" -> "Finalizada";
      default -> "Sin estado";
    };
  }

  public String getAvanceTexto() {
    return Formato.porcentaje(porcentajeAvance);
  }

  public String getVelocidadTexto() {
    return velocidadKmh == null ? "Sin dato" : Formato.velocidad(velocidadKmh);
  }

  public String getUltimoReporteTexto() {
    String texto = Formato.fechaHoraCorta(ultimoReporte);
    return texto == null ? "Sin reportes de posición" : texto;
  }
}
