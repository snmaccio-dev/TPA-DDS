package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.Comprobante;

public record ComprobanteDTO(
    long donacionId,
    String fecha,
    String patenteCamion,
    String razonSocialBeneficiaria,
    String nombreDonante,
    String descripcionDonacion
) {

  public static ComprobanteDTO desde(Comprobante comprobante) {
    return new ComprobanteDTO(
        comprobante.donacionId(),
        Fechas.aTexto(comprobante.fecha()),
        comprobante.patenteCamion(),
        comprobante.razonSocialBeneficiaria(),
        comprobante.nombreDonante(),
        comprobante.descripcionDonacion()
    );
  }
}