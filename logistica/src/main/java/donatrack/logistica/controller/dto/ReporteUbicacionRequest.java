package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.monitoreo.ReporteUbicacion;

import java.time.LocalDateTime;

public record ReporteUbicacionRequest(
    long rutaId,
    String patenteCamion,
    double latitud,
    double longitud,
    double velocidadKmh,
    String momento
) {

  public ReporteUbicacion aReporte() {
    LocalDateTime medido = Fechas.aFechaHora(momento);

    return new ReporteUbicacion(
        rutaId,
        patenteCamion,
        latitud,
        longitud,
        velocidadKmh,
        medido == null ? LocalDateTime.now() : medido
    );
  }
}
