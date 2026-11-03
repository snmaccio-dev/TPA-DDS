package donatrack.logistica.contrato;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.List;

public record EventoInicioRuta(
    long rutaId,
    List<Long> donacionIds,
    String linkMapa
) {

  public EventoInicioRuta {
    if (donacionIds == null || donacionIds.isEmpty()) {
      throw new DatosInvalidosException("El evento debe indicar al menos una donacion.");
    }
    if (linkMapa == null || linkMapa.isBlank()) {
      throw new DatosInvalidosException("El evento de inicio de ruta debe incluir el link al mapa.");
    }
    donacionIds = List.copyOf(donacionIds);
  }
}
