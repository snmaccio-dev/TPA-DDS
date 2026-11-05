package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.DatosInvalidosException;

public record IniciarRutaRequest(Long choferId) {

  public long choferIdObligatorio() {
    if (choferId == null) {
      throw new DatosInvalidosException("Debe indicarse el chofer que inicia la ruta.");
    }
    return choferId;
  }
}
