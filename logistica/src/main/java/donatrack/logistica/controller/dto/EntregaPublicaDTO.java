package donatrack.logistica.controller.dto;

import donatrack.logistica.contrato.Fechas;
import donatrack.logistica.domain.entrega.Entrega;

import java.util.List;

public record EntregaPublicaDTO(
    long entregaId,
    long donacionId,
    long beneficiariaId,
    String descripcion,
    double cantidad,
    String unidad,
    String fechaHoraEntrega,
    List<String> fotos
) {

  public static EntregaPublicaDTO desde(Entrega entrega) {
    return new EntregaPublicaDTO(
        entrega.getId(),
        entrega.getDonacionId(),
        entrega.getBeneficiariaId(),
        entrega.getDescripcionDonacion(),
        entrega.getCantidad(),
        entrega.getUnidad(),
        Fechas.aTexto(entrega.getFechaHoraEntrega()),
        entrega.getFotos()
    );
  }

  public static List<EntregaPublicaDTO> desde(List<Entrega> entregas) {
    return entregas.stream().map(EntregaPublicaDTO::desde).toList();
  }
}
