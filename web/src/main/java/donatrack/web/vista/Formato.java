package donatrack.web.vista;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Formato {

  private static final Locale LOCAL = new Locale("es", "AR");
  private static final DateTimeFormatter CORTA =
      DateTimeFormatter.ofPattern("dd/MM · HH:mm");

  private Formato() {
  }

  public static String kilos(double valor) {
    return String.format(LOCAL, "%,.0f kg", valor);
  }

  public static String metrosCubicos(double valor) {
    return String.format(LOCAL, "%,.1f m³", valor);
  }

  public static String metros(double valor) {
    return String.format(LOCAL, "%,.2f m", valor);
  }

  public static String porcentaje(double valor) {
    return String.format(LOCAL, "%.0f%%", valor);
  }

  public static String velocidad(double valor) {
    return String.format(LOCAL, "%.0f km/h", valor);
  }

  public static String cantidad(double valor, String unidad) {
    return String.format(LOCAL, "%,.1f %s", valor, unidad);
  }

  public static String fechaHoraCorta(String fechaHoraIso) {
    if (fechaHoraIso == null || fechaHoraIso.isBlank()) {
      return null;
    }

    try {
      return LocalDateTime.parse(fechaHoraIso).format(CORTA);
    } catch (DateTimeParseException e) {
      return fechaHoraIso;
    }
  }
}
