package donatrack.logistica.contrato;

import java.time.LocalDateTime;

public record EventoEntrega(
    long donacionId,
    ResultadoEntrega resultado,
    String fechaHora,
    String patenteCamion,
    String motivo
) {

  public EventoEntrega {
    if (resultado == null) {
      throw new IllegalArgumentException("El evento de entrega debe indicar un resultado.");
    }
    if (fechaHora == null || fechaHora.isBlank()) {
      throw new IllegalArgumentException("El evento de entrega debe indicar la fecha y hora.");
    }
    Fechas.aFechaHora(fechaHora);
    if (resultado == ResultadoEntrega.ENTREGADA
        && (patenteCamion == null || patenteCamion.isBlank())) {
      throw new IllegalArgumentException("El evento de entrega debe indicar el camion responsable.");
    }
    if (resultado == ResultadoEntrega.NO_RECIBIDA && (motivo == null || motivo.isBlank())) {
      throw new IllegalArgumentException("Una entrega no recibida debe indicar el motivo.");
    }
  }

  public static EventoEntrega de(long donacionId,
                                 ResultadoEntrega resultado,
                                 LocalDateTime fechaHora,
                                 String patenteCamion,
                                 String motivo) {
    return new EventoEntrega(
        donacionId,
        resultado,
        Fechas.aTexto(fechaHora),
        patenteCamion,
        motivo
    );
  }
}