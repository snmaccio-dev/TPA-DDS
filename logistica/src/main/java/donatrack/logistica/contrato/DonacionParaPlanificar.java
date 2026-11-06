package donatrack.logistica.contrato;

import donatrack.logistica.domain.DatosInvalidosException;

public record DonacionParaPlanificar(
    long donacionId,
    String descripcion,
    double cantidad,
    String unidad,
    long beneficiariaId,
    String razonSocialBeneficiaria,
    String direccionDestino,
    Long donanteId
) {

  public DonacionParaPlanificar {
    if (descripcion == null || descripcion.isBlank()) {
      throw new DatosInvalidosException("La donacion a planificar debe tener descripcion.");
    }
    if (direccionDestino == null || direccionDestino.isBlank()) {
      throw new DatosInvalidosException("La donacion a planificar debe tener direccion de destino.");
    }
  }
}