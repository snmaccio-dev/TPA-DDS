package donatrack.logistica.controller.dto;

public record IniciarRutaRequest(Long choferId) {

  public long choferIdObligatorio() {
    if (choferId == null) {
      throw new IllegalArgumentException("Debe indicarse el chofer que inicia la ruta.");
    }
    return choferId;
  }
}
