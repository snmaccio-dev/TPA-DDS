package donatrack.donaciones.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlanificadorAsignaciones {

    public static final LocalTime HORA_POR_DEFECTO = LocalTime.of(3, 0);
    private static final long PERIODO_HORAS = 24;

    private final GestorAsignaciones gestor;
    private final LocalTime hora;
    private final ScheduledExecutorService executor;

    public PlanificadorAsignaciones(GestorAsignaciones gestor, LocalTime hora) {
        this.gestor = gestor;
        this.hora = hora;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "planificador-asignaciones");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public PlanificadorAsignaciones(GestorAsignaciones gestor) {
        this(gestor, HORA_POR_DEFECTO);
    }

    public void iniciar() {
        long delayInicial = calcularDelayHastaProximaEjecucion();
        executor.scheduleAtFixedRate(
            this::ejecutar,
            delayInicial,
            TimeUnit.HOURS.toSeconds(PERIODO_HORAS),
            TimeUnit.SECONDS
        );
    }

    public void detener() {
        executor.shutdown();
    }

    private void ejecutar() {
        // scheduleAtFixedRate cancela silenciosamente todas las corridas futuras si la
        // tarea lanza una excepción — atrapar Throwable para que el job siga vivo.
        try {
            int procesadas = gestor.ejecutarMatchmakingProgramado();
            System.out.println("[MATCHMAKING] Corrida programada — donaciones procesadas: " + procesadas);
        } catch (Throwable t) {
            System.err.println("[MATCHMAKING] Error en corrida programada: " + t.getMessage());
        }
    }

    private long calcularDelayHastaProximaEjecucion() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime proxima = LocalDateTime.of(LocalDate.now(), hora);
        if (!proxima.isAfter(ahora)) {
            proxima = proxima.plusDays(1);
        }
        return Duration.between(ahora, proxima).getSeconds();
    }
}