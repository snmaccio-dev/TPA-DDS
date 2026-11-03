package donatrack.donaciones.contrato;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Fechas {

  private Fechas() {
  }

  public static String aTexto(LocalDateTime fechaHora) {
    return fechaHora == null
        ? null
        : fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}
