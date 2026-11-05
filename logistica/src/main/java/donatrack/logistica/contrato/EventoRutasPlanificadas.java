package donatrack.logistica.contrato;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.List;

public record EventoRutasPlanificadas(
    long rutaId,
    List<Long> donacionIds
) {

  public EventoRutasPlanificadas {
    if (donacionIds == null || donacionIds.isEmpty()) {
      throw new DatosInvalidosException("El evento debe indicar al menos una donacion.");
    }
    donacionIds = List.copyOf(donacionIds);
  }
}
