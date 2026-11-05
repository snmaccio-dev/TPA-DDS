package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.ruta.DestinoEntrega;

import java.util.List;

public record DestinoDTO(
    String direccion,
    List<EntregaDTO> entregas
) {

  public static DestinoDTO desde(DestinoEntrega destino, List<Entrega> entregasDeLaRuta) {
    return new DestinoDTO(
        destino.getDireccion(),
        EntregaDTO.desde(entregasDeLaRuta.stream()
            .filter(entrega -> Integer.valueOf(destino.getOrden()).equals(entrega.getOrdenDestino()))
            .toList())
    );
  }
}
