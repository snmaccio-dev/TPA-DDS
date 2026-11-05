package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.ruta.DestinoEntrega;

import java.util.List;

public record DestinoDTO(
    String direccion,
    List<EntregaDTO> entregas
) {

  public static DestinoDTO desde(DestinoEntrega destino) {
    return new DestinoDTO(
        destino.getDireccion(),
        EntregaDTO.desde(destino.getEntregas())
    );
  }
}
