package donatrack.logistica.controller.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Fechas {

  private Fechas() {
  }

  public static String aTexto(LocalDateTime fechaHora) {
    return fechaHora == null
        ? null
        : fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  public static LocalDateTime aFechaHora(String texto) {
    if (texto == null || texto.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "La fecha '" + texto + "' no respeta el formato ISO-8601 (2026-09-09T14:30:00)."
      );
    }
  }
}
