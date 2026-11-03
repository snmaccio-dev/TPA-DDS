package donatrack.logistica.contrato;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.List;

public record DestinoPlanificado(
    String direccion,
    List<Long> entregaIds
) {

  public DestinoPlanificado {
    if (direccion == null || direccion.isBlank()) {
      throw new DatosInvalidosException("El destino planificado debe tener una direccion.");
    }
    if (entregaIds == null || entregaIds.isEmpty()) {
      throw new DatosInvalidosException("El destino planificado debe tener al menos una entrega.");
    }
    entregaIds = List.copyOf(entregaIds);
  }
}
