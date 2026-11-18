
package donatrack.web.vista;

import java.util.List;

public record DonacionVista(
    Long donacionId,
    String descripcion,
    double cantidad,
    String unidad,
    Long beneficiariaId,
    String razonSocialBeneficiaria,
    String direccionDestino,
    Long donanteId,
    String estado,
    String subcategoria,
    String categoria,
    String fechaHoraEntrega,
    List<Bien> bienes,
    String donanteNombre,
    String donanteDocumento
) {

  public record Bien(String descripcion, String subcategoria, double cantidad,
                     String condicion, String fechaVencimiento) {
    public String getDescripcion() { return descripcion; }
    public String getSubcategoria() { return subcategoria; }
    public String getCantidad() { return String.valueOf(cantidad); }
    public String getCondicion() { return condicion; }
    public String getFechaVencimiento() { return fechaVencimiento; }
  }

  public DonacionVista {
    bienes = bienes == null ? List.of() : List.copyOf(bienes);
  }

  public Long getId() { return donacionId; }
  public String getDescripcion() { return descripcion; }
  public String getCategoria() { return categoria; }
  public String getSubcategoria() { return subcategoria; }
  public String getRazonSocialBeneficiaria() { return razonSocialBeneficiaria; }
  public String getDireccionDestino() { return direccionDestino; }
  public String getEstado() { return estado; }
  public String getDonanteNombre() { return donanteNombre; }
  public List<Bien> getBienes() { return bienes; }

  public String getCantidadTexto() {
    return Formato.cantidad(cantidad, unidad == null ? "" : unidad.toLowerCase());
  }

  public String getFechaHoraEntregaTexto() {
    return Formato.fechaHoraCorta(fechaHoraEntrega);
  }

  public String getEstadoTexto() {
    if (estado == null) return "";
    return switch (estado) {
      case "EN_DEPOSITO" -> "En depósito";
      case "ASIGNACION_REALIZADA" -> "Asignada";
      case "LISTA_PARA_ENTREGAR" -> "Lista para entregar";
      case "PENDIENTE_RECEPCION" -> "Pendiente de recepción";
      case "EN_TRASLADO" -> "En traslado";
      case "ENTREGADA" -> "Entregada";
      case "ENTREGA_FALLIDA" -> "Entrega fallida";
      case "VENCIDA" -> "Vencida";
      default -> estado;
    };
  }

  public String getEstadoClase() {
    if (estado == null) return "info";
    return switch (estado) {
      case "ENTREGADA" -> "success";
      case "ENTREGA_FALLIDA", "VENCIDA" -> "danger";
      case "LISTA_PARA_ENTREGAR" -> "warning";
      default -> "info";
    };
  }

  public boolean isDisponible() {
    return "EN_DEPOSITO".equals(estado);
  }

  public boolean isPendienteRecepcion() {
    return "PENDIENTE_RECEPCION".equals(estado);
  }

  public boolean isVencida() {
    return "VENCIDA".equals(estado);
  }

  // Nuevo

  public String getDonanteIdentificacion() {
    if (donanteNombre != null && !donanteNombre.isBlank()) {
      return donanteNombre;
    }

    if (donanteDocumento != null && !donanteDocumento.isBlank()) {
      return "Documento: " + donanteDocumento;
    }

    return donanteId == null
        ? "Donante no identificado"
        : "Donante #" + donanteId;
  }
}
