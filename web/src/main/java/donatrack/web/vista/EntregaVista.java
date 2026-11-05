package donatrack.web.vista;

import java.util.List;

public record EntregaVista(
    long id,
    long donacionId,
    long beneficiariaId,
    String direccionDestino,
    String descripcionDonacion,
    double cantidad,
    String unidad,
    Double pesoKg,
    Double volumenM3,
    String estado,
    Long rutaId,
    String patenteCamion,
    String fechaHoraEntrega,
    List<String> fotos,
    String motivoNoRecepcion
) {

  private static final String EN_TRASLADO = "EN_TRASLADO";

  public long getId() {
    return id;
  }

  public long getDonacionId() {
    return donacionId;
  }

  public String getDireccionDestino() {
    return direccionDestino;
  }

  public String getDescripcionDonacion() {
    return descripcionDonacion;
  }

  public String getCantidadTexto() {
    return Formato.cantidad(cantidad, unidad == null ? "" : unidad);
  }

  public String getPatenteCamion() {
    return patenteCamion == null ? "Sin asignar" : patenteCamion;
  }

  public boolean isConfirmable() {
    return EN_TRASLADO.equals(estado);
  }

  public String getEstadoTexto() {
    return switch (estado == null ? "" : estado) {
      case "PENDIENTE" -> "Pendiente de planificación";
      case "PLANIFICADA" -> "Planificada";
      case EN_TRASLADO -> "En tránsito";
      case "ENTREGADA" -> "Entregada";
      case "NO_RECIBIDA" -> "No recibida";
      default -> "Sin estado";
    };
  }

  public String getEstadoEstilo() {
    return switch (estado == null ? "" : estado) {
      case "ENTREGADA" -> "success";
      case "NO_RECIBIDA" -> "danger";
      case EN_TRASLADO -> "info";
      default -> "warning";
    };
  }

  public String getFechaHoraEntregaTexto() {
    return Formato.fechaHoraCorta(fechaHoraEntrega);
  }

  public List<String> getFotos() {
    return fotos == null ? List.of() : fotos;
  }

  public String getMotivoNoRecepcion() {
    return motivoNoRecepcion;
  }
}
