package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.monitoreo.ReporteUbicacion;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record ReporteUbicacionRequest(
    long rutaId,
    String patenteCamion,
    double latitud,
    double longitud,
    double velocidadKmh,
    String momento
) {

  public ReporteUbicacion aReporte(RutaReparto ruta) {
    return new ReporteUbicacion(
        ruta,
        patenteCamion,
        latitud,
        longitud,
        velocidadKmh,
        momentoMedido()
    );
  }

  private LocalDateTime momentoMedido() {
    if (momento == null || momento.isBlank()) {
      return LocalDateTime.now();
    }
    try {
      return LocalDateTime.parse(momento, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException e) {
      throw new DatosInvalidosException(
          "La fecha '" + momento + "' no respeta el formato ISO-8601 (2026-09-09T14:30:00)."
      );
    }
  }
}
