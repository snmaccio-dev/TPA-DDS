package donatrack.logistica.contrato;

public record DonacionParaPlanificar(
    long donacionId,
    String descripcion,
    String subcategoria,
    double cantidad,
    String unidad,
    double pesoKg,
    double volumenM3,
    long beneficiariaId,
    String razonSocialBeneficiaria,
    String direccionDestino
) {

  public DonacionParaPlanificar {
    if (descripcion == null || descripcion.isBlank()) {
      throw new IllegalArgumentException("La donacion a planificar debe tener descripcion.");
    }
    if (direccionDestino == null || direccionDestino.isBlank()) {
      throw new IllegalArgumentException("La donacion a planificar debe tener direccion de destino.");
    }
    if (pesoKg < 0) {
      throw new IllegalArgumentException("El peso de la donacion no puede ser negativo.");
    }
    if (volumenM3 < 0) {
      throw new IllegalArgumentException("El volumen de la donacion no puede ser negativo.");
    }
  }
}
