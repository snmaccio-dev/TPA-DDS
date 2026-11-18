package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.List;

// Los primeros campos mantienen el contrato utilizado por Logística.
// Los campos adicionales son consumidos por la aplicación web.
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
    String categoria,
    String fechaHoraEntrega,
    List<BienDTO> bienes,
    String fotoUrl,
    String donanteNombre
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
        donacion.getSubcategoria().getCategoria() == null
            ? null
            : donacion.getSubcategoria().getCategoria().getNombre(),
        Fechas.aTexto(donacion.getFechaHoraEntrega()),
        donacion.getBienes().stream().map(BienDTO::desde).toList(),
        donacion.getFotoUrl(),
        donacion.getDonante() == null
            ? null
            : donacion.getDonante().getPersona().getNombreDisplay()
    );
  }

  public static List<DonacionDTO> desde(List<Donacion> donaciones) {
    return donaciones.stream().map(DonacionDTO::desde).toList();
  }
}

