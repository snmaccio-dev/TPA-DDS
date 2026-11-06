package donatrack.web.vista;

public record NecesidadVista(
    Long id,
    String descripcion,
    int cantidad,
    String subcategoria,
    String unidad,
    boolean esExtraordinaria,
    String cuitBeneficiaria
) {

  public Long getId() {
    return id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getSubcategoria() {
    return subcategoria;
  }

  public String getCantidadTexto() {
    return cantidad + " " + (unidad == null ? "" : unidad.toLowerCase());
  }

  public String getTipoTexto() {
    return esExtraordinaria ? "Extraordinaria" : "Recurrente";
  }

  // La pastilla de estado usa estas clases en el CSS de la maqueta.
  public String getTipoClase() {
    return esExtraordinaria ? "warning" : "info";
  }
}
