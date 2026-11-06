package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.List;

// Los 8 primeros campos (donacionId..donanteId) tienen el mismo nombre y tipo que
// el record `DonacionParaPlanificar` de logistica, para que el mismo endpoint sirva a
// Logistica y al front. El SerializadorJackson del otro lado ignora los campos extra.
// donanteId lo usa Logistica para autorizar: lo guarda en la entrega y con eso valida
// que una persona donante solo vea el seguimiento de sus propias donaciones.
public record DonacionDTO(
    long donacionId,
    String descripcion,
    double cantidad,
    String unidad,
    Long beneficiariaId,
    String razonSocialBeneficiaria,
    String direccionDestino,
    Long donanteId,
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
        donacion.getDonante() == null ? null : donacion.getDonante().getId(),
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