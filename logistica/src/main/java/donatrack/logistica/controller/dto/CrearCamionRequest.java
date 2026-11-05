package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.flota.Camion;

public record CrearCamionRequest(
    String patente,
    double capacidadVolumen,
    double altura,
    double capacidadCarga
) {

  public Camion aCamion() {
    if (patente == null || patente.isBlank()) {
      throw new IllegalArgumentException("El camion debe tener una patente.");
    }
    return new Camion(patente, capacidadVolumen, altura, capacidadCarga);
  }
}
