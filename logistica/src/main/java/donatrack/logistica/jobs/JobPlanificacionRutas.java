package donatrack.logistica.jobs;

import donatrack.logistica.service.GestorLogistica;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JobPlanificacionRutas {

  private static final long UN_DIA_EN_SEGUNDOS = TimeUnit.DAYS.toSeconds(1);

  private final GestorLogistica gestorLogistica;
  private final LocalTime horario;

  private final ScheduledExecutorService planificador =
      Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "planificacion-nocturna");
        hilo.setDaemon(true);
        return hilo;
      });

  public JobPlanificacionRutas(GestorLogistica gestorLogistica, LocalTime horario) {
    if (gestorLogistica == null) {
      throw new IllegalArgumentException("Debe indicarse el gestor de logistica.");
    }
    if (horario == null) {
      throw new IllegalArgumentException("Debe indicarse el horario de la corrida.");
    }
    this.gestorLogistica = gestorLogistica;
    this.horario = horario;
  }

  public static LocalTime horarioDesde(String texto, LocalTime valorPorDefecto) {
    if (texto == null || texto.isBlank()) {
      return valorPorDefecto;
    }
    try {
      return LocalTime.parse(texto);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "El horario de planificacion '" + texto + "' no tiene el formato HH:mm."
      );
    }
  }

  public void iniciar() {
    long demora = segundosHastaLaProximaCorrida();

    planificador.scheduleAtFixedRate(
        this::correr,
        demora,
        UN_DIA_EN_SEGUNDOS,
        TimeUnit.SECONDS
    );

    System.out.println("[JOB] Planificacion diaria a las " + horario
        + ". Primera corrida en " + Duration.ofSeconds(demora).toMinutes() + " minuto(s).");
  }

  public void detener() {
    planificador.shutdownNow();
  }

  private void correr() {
    try {
      List<String> solicitudes = gestorLogistica.ejecutarCorrida();
      System.out.println("[JOB] Corrida completada. Solicitudes enviadas: " + solicitudes.size());
    } catch (RuntimeException e) {
      System.out.println("[JOB] La corrida fallo: " + e.getMessage()
          + ". Se reintenta en la proxima ejecucion programada.");
    }
  }

  private long segundosHastaLaProximaCorrida() {
    LocalDateTime ahora = LocalDateTime.now();
    LocalDateTime proxima = ahora.toLocalDate().atTime(horario);

    if (!proxima.isAfter(ahora)) {
      proxima = proxima.plusDays(1);
    }

    return Duration.between(ahora, proxima).getSeconds();
  }
}
