package donatrack.logistica.contrato;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.List;

public record RutaPlanificada(
    String patenteCamion,
    List<DestinoPlanificado> destinos
) {

  public RutaPlanificada {
    if (patenteCamion == null || patenteCamion.isBlank()) {
      throw new DatosInvalidosException("La ruta planificada debe indicar un camion.");
    }
    if (destinos == null || destinos.isEmpty()) {
      throw new DatosInvalidosException("La ruta planificada debe tener al menos un destino.");
    }
    destinos = List.copyOf(destinos);
  }
}
