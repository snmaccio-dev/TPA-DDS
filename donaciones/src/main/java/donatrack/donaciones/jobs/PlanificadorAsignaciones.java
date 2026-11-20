package donatrack.donaciones.jobs;

import donatrack.donaciones.service.AsignacionesService;
import donatrack.donaciones.service.ResultadoCorrida;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

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

    private final AsignacionesService service;
    private final LocalTime hora;
    private final ScheduledExecutorService executor;

    public PlanificadorAsignaciones(AsignacionesService service, LocalTime hora) {
        this.service = service;
        this.hora = hora;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "planificador-asignaciones");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public PlanificadorAsignaciones(AsignacionesService service) {
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
        System.out.println("[JOB ASIGNACIONES] Corrida diaria a las " + hora
            + ". Primera corrida en " + Duration.ofSeconds(delayInicial).toMinutes() + " minuto(s).");
    }

    public void detener() {
        executor.shutdown();
    }

    private void ejecutar() {
        // scheduleAtFixedRate cancela silenciosamente todas las corridas futuras si la
        // tarea lanza una excepción — atrapar RuntimeException para que el job siga vivo.
        //
        // El hilo del job vive mientras viva la app y jpa-extras guarda un EntityManager
        // por hilo: sin dispose() ese EntityManager no se cerraba nunca y cada corrida
        // diaria reutilizaba el de la anterior, con sus entidades ya cargadas (la lectura
        // de donaciones en deposito podia devolver estados viejos) y la conexion tomada
        // por las lecturas fuera de transaccion.
        System.out.println("[JOB ASIGNACIONES] Inicio de la corrida programada.");
        try {
            ResultadoCorrida resultado = service.ejecutarMatchmakingProgramado();
            System.out.println("[JOB ASIGNACIONES] Corrida terminada. Donaciones procesadas: "
                + resultado.exitosas() + ", fallidas: " + resultado.cantidadDeFallas());
            resultado.fallidas().forEach(falla ->
                System.err.println("[JOB ASIGNACIONES] Fallida: " + falla));
        } catch (RuntimeException e) {
            System.err.println("[JOB ASIGNACIONES] La corrida fallo: " + e.getMessage()
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