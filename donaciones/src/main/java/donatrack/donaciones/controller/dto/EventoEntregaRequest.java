package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.DatosInvalidosException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record EventoEntregaRequest(
    long donacionId,
    String resultado,
    String fechaHora,
    String patenteCamion,
    String motivo
) {

  public LocalDateTime fechaHoraMedida() {
    if (fechaHora == null || fechaHora.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(fechaHora, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException e) {
      throw new DatosInvalidosException(
          "La fecha '" + fechaHora + "' no respeta el formato ISO-8601 (2026-09-09T14:30:00)."
      );
    }
  }
}
