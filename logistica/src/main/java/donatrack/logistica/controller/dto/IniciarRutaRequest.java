package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.flota.Chofer;

public record IniciarRutaRequest(
    String nombre,
    String apellido,
    String documento,
    String licencia
) {

  public Chofer aChofer() {
    return new Chofer(nombre, apellido, documento, licencia);
  }
}
