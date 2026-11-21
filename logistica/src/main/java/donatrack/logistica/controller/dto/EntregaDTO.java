package donatrack.logistica.controller.dto;

import donatrack.logistica.contrato.Fechas;
import donatrack.logistica.domain.entrega.Entrega;

import java.util.List;

public record EntregaDTO(
    long id,
    long donacionId,
    long beneficiariaId,
    String direccionDestino,
    String descripcionDonacion,
    double cantidad,
    String unidad,
    Double pesoKg,
    Double volumenM3,
    String estado,
    Long rutaId,
    String patenteCamion,
    String fechaHoraEntrega,
    List<String> fotos,
    String motivoNoRecepcion,
    // Momento en que el camion salio con la ruta; null mientras la ruta no se inicio.
    String fechaHoraSalida
) {

  public static EntregaDTO desde(Entrega entrega) {
    return new EntregaDTO(
        entrega.getId(),
        entrega.getDonacionId(),
        entrega.getBeneficiariaId(),
        entrega.getDireccionDestino(),
        entrega.getDescripcionDonacion(),
        entrega.getCantidad(),
        entrega.getUnidad(),
        entrega.getPesoKg(),
        entrega.getVolumenM3(),
        entrega.getEstado().getNombre(),
        entrega.getRuta() == null ? null : entrega.getRuta().getId(),
        entrega.getPatenteCamion(),
        Fechas.aTexto(entrega.getFechaHoraEntrega()),
        entrega.getFotos(),
        entrega.getMotivoNoRecepcion(),
        entrega.getRuta() == null
            ? null
            : Fechas.aTexto(entrega.getRuta().getFechaHoraInicio())
    );
  }

  public static List<EntregaDTO> desde(List<Entrega> entregas) {
    return entregas.stream().map(EntregaDTO::desde).toList();
  }
}