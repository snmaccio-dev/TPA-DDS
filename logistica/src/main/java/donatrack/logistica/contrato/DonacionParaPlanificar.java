package donatrack.logistica.contrato;

public record DonacionParaPlanificar(
    long donacionId,
    String descripcion,
    double cantidad,
    String unidad,
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
  }
}