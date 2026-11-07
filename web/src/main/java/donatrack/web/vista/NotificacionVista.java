package donatrack.web.vista;

public record NotificacionVista(
    Long id,
    String tipo,
    String mensaje,
    String fechaHora,
    boolean leida,
    Long donacionId
) {

  public Long getId() { return id; }
  public String getTipo() { return tipo; }
  public String getMensaje() { return mensaje; }
  public Long getDonacionId() { return donacionId; }
  public boolean isLeida() { return leida; }

  public String getFechaHoraTexto() {
    return Formato.fechaHoraCorta(fechaHora);
  }

  public String getTipoTexto() {
    if (tipo == null) return "";
    String limpio = tipo.replace('_', ' ').toLowerCase();
    return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
  }

  public String getEstadoClase() {
    return leida ? "info" : "warning";
  }

  public String getEstadoTexto() {
    return leida ? "Leída" : "Nueva";
  }
}
