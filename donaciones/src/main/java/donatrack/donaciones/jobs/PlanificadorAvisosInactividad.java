package donatrack.donaciones.jobs;

import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.ResultadoCorrida;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlanificadorAvisosInactividad {

    // 09:00, no de madrugada: es un aviso al donante y no conviene apilarlo con el
    // matchmaking programado.
    public static final LocalTime HORA_POR_DEFECTO = LocalTime.of(9, 0);
    private static final long PERIODO_HORAS = 24;

    private final DonantesService gestor;
    private final LocalTime hora;
    private final ScheduledExecutorService executor;

    public PlanificadorAvisosInactividad(DonantesService gestor, LocalTime hora) {
        this.gestor = gestor;
        this.hora = hora;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "planificador-avisos-inactividad");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public PlanificadorAvisosInactividad(DonantesService gestor) {
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
        // tarea lanza una excepciÃ³n â€” atrapar RuntimeException para que el job siga vivo.
        try {
            ResultadoCorrida resultado = gestor.avisarInactivos();
            System.out.println("[AVISO-INACTIVIDAD] Corrida programada â€” donantes avisados: "
                + resultado.exitosas() + ", fallidos: " + resultado.cantidadDeFallas());
            resultado.fallidas().forEach(falla ->
                System.err.println("[AVISO-INACTIVIDAD] " + falla));
        } catch (RuntimeException e) {
            System.err.println("[AVISO-INACTIVIDAD] Error en corrida programada: " + e.getMessage());
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