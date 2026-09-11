package donatrack.logistica.contrato;

import java.util.List;

public record EventoRutasPlanificadas(
    long rutaId,
    List<Long> donacionIds
) {

  public EventoRutasPlanificadas {
    if (donacionIds == null || donacionIds.isEmpty()) {
      throw new IllegalArgumentException("El evento debe indicar al menos una donacion.");
    }
    donacionIds = List.copyOf(donacionIds);
  }
}
