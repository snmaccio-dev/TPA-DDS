package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.entrega.Entrega;

import java.util.List;

public record EntregaDTO(
    long id,
    long donacionId,
    long beneficiariaId,
    String direccionDestino,
    String descripcionDonacion,
    double pesoKg,
    double volumenM3,
    String estado,
    Long rutaId,
    String patenteCamion,
    String fechaHoraEntrega,
    List<String> fotos,
    String motivoNoRecepcion
) {

  public static EntregaDTO desde(Entrega entrega) {
    return new EntregaDTO(
        entrega.getId(),
        entrega.getDonacionId(),
        entrega.getBeneficiariaId(),
        entrega.getDireccionDestino(),
        entrega.getDescripcionDonacion(),
        entrega.getPesoKg(),
        entrega.getVolumenM3(),
        entrega.getEstado().getNombre(),
        entrega.getRuta() == null ? null : entrega.getRuta().getId(),
        entrega.getPatenteCamion(),
        Fechas.aTexto(entrega.getFechaHoraEntrega()),
        entrega.getFotos(),
        entrega.getMotivoNoRecepcion()
    );
  }

  public static List<EntregaDTO> desde(List<Entrega> entregas) {
    return entregas.stream().map(EntregaDTO::desde).toList();
  }
}
