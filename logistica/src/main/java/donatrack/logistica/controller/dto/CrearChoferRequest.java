package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.flota.Chofer;

public record CrearChoferRequest(
    String nombre,
    String apellido,
    String documento,
    String licencia,
    String keycloakId
) {

  public Chofer aChofer() {
    Chofer chofer = new Chofer(nombre, apellido, documento, licencia);

    if (keycloakId != null && !keycloakId.isBlank()) {
      chofer.vincularConCuenta(keycloakId);
    }

    return chofer;
  }
}
