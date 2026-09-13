package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.List;

// Los 7 primeros campos (donacionId..direccionDestino) tienen el mismo nombre y tipo que
// el record `DonacionParaPlanificar` de logistica, para que el mismo endpoint sirva a
// Logistica y al front. El SerializadorJackson del otro lado ignora los campos extra.
public record DonacionDTO(
    long donacionId,
    String descripcion,
    double cantidad,
    String unidad,
    Long beneficiariaId,
    String razonSocialBeneficiaria,
    String direccionDestino,
    String estado,
    String subcategoria,
    String fechaHoraEntrega,
    List<BienDTO> bienes
) {

  public static DonacionDTO desde(Donacion donacion) {
    Beneficiaria destinatario = donacion.getDestinatarioAsignado();
    return new DonacionDTO(
        donacion.getId(),
        donacion.getDescripcion(),
        donacion.getCantidadTotal(),
        donacion.getUnidades().name(),
        destinatario == null ? null : destinatario.getId(),
        destinatario == null ? null : destinatario.getPersona().getRazonSocial(),
        destinatario == null ? null : destinatario.getPersona().getDireccion(),
        donacion.getEstado().getNombre(),
        donacion.getSubcategoria().getNombre(),
        Fechas.aTexto(donacion.getFechaHoraEntrega()),
        donacion.getBienes().stream().map(BienDTO::desde).toList()
    );
  }

  public static List<DonacionDTO> desde(List<Donacion> donaciones) {
    return donaciones.stream().map(DonacionDTO::desde).toList();
  }
}