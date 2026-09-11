package donatrack.logistica.contrato;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record EventoEntrega(
    long donacionId,
    ResultadoEntrega resultado,
    String fechaHora,
    String patenteCamion,
    List<String> fotos,
    String motivo
) {

  public EventoEntrega {
    if (resultado == null) {
      throw new IllegalArgumentException("El evento de entrega debe indicar un resultado.");
    }
    if (fechaHora == null || fechaHora.isBlank()) {
      throw new IllegalArgumentException("El evento de entrega debe indicar la fecha y hora.");
    }
    if (patenteCamion == null || patenteCamion.isBlank()) {
      throw new IllegalArgumentException("El evento de entrega debe indicar el camion responsable.");
    }
    if (resultado == ResultadoEntrega.NO_RECIBIDA && (motivo == null || motivo.isBlank())) {
      throw new IllegalArgumentException("Una entrega no recibida debe indicar el motivo.");
    }
    fotos = fotos == null ? List.of() : List.copyOf(fotos);
  }

  public static EventoEntrega de(long donacionId,
                                 ResultadoEntrega resultado,
                                 LocalDateTime fechaHora,
                                 String patenteCamion,
                                 List<String> fotos,
                                 String motivo) {
    return new EventoEntrega(
        donacionId,
        resultado,
        fechaHora == null ? null : fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        patenteCamion,
        fotos,
        motivo
    );
  }
}
