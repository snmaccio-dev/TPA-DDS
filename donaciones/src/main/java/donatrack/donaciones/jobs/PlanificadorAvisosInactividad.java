package donatrack.donaciones.jobs;

import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.ResultadoCorrida;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

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

    private final DonantesService service;
    private final LocalTime hora;
    private final ScheduledExecutorService executor;

    public PlanificadorAvisosInactividad(DonantesService service, LocalTime hora) {
        this.service = service;
        this.hora = hora;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "planificador-avisos-inactividad");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public PlanificadorAvisosInactividad(DonantesService service) {
        this(service, HORA_POR_DEFECTO);
    }

    public void iniciar() {
        long delayInicial = calcularDelayHastaProximaEjecucion();
        executor.scheduleAtFixedRate(
            this::ejecutar,
            delayInicial,
            TimeUnit.HOURS.toSeconds(PERIODO_HORAS),
            TimeUnit.SECONDS
        );
        System.out.println("[JOB AVISOS-INACTIVIDAD] Corrida diaria a las " + hora
            + ". Primera corrida en " + Duration.ofSeconds(delayInicial).toMinutes() + " minuto(s).");
    }

    public void detener() {
        executor.shutdown();
    }

    private void ejecutar() {
        // scheduleAtFixedRate cancela silenciosamente todas las corridas futuras si la
        // tarea lanza una excepción — atrapar RuntimeException para que el job siga vivo.
        //
        // Mismo motivo que en PlanificadorAsignaciones: el EntityManager del hilo del job
        // se cierra al final de cada corrida.
        System.out.println("[JOB AVISOS-INACTIVIDAD] Inicio de la corrida programada.");
        try {
            ResultadoCorrida resultado = service.avisarInactivos();
            System.out.println("[JOB AVISOS-INACTIVIDAD] Corrida terminada. Donantes avisados: "
                + resultado.exitosas() + ", fallidos: " + resultado.cantidadDeFallas());
            resultado.fallidas().forEach(falla ->
                System.err.println("[JOB AVISOS-INACTIVIDAD] Fallido: " + falla));
        } catch (RuntimeException e) {
            System.err.println("[JOB AVISOS-INACTIVIDAD] La corrida fallo: " + e.getMessage()
                + ". Se reintenta en la proxima ejecucion programada.");
            e.printStackTrace();
        } finally {
            WithSimplePersistenceUnit.dispose();
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