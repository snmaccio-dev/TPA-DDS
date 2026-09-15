package donatrack.logistica;

import donatrack.logistica.controller.CamionesController;
import donatrack.logistica.controller.EntregasController;
import donatrack.logistica.controller.MonitoreoController;
import donatrack.logistica.controller.PlanificacionController;
import donatrack.logistica.controller.RutasCallbackController;
import donatrack.logistica.controller.RutasController;
import donatrack.logistica.controller.dto.ErrorDTO;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;
import donatrack.logistica.domain.planificacion.GeneradorRutas;
import donatrack.logistica.infrastructure.integracion.ClienteDonacionesHttp;
import donatrack.logistica.infrastructure.integracion.ClienteDonacionesSimulado;
import donatrack.logistica.infrastructure.integracion.SerializadorJackson;
import donatrack.logistica.infrastructure.planificacion.GeneradorRutasExterno;
import donatrack.logistica.infrastructure.planificacion.GeneradorRutasSimulado;
import donatrack.logistica.jobs.JobPlanificacionRutas;
import donatrack.logistica.service.GestorCamiones;
import donatrack.logistica.service.GestorEntregas;
import donatrack.logistica.service.GestorLogistica;
import donatrack.logistica.service.GestorMonitoreo;
import donatrack.logistica.service.GestorRutas;
import donatrack.logistica.service.RecursoInexistenteException;
import io.javalin.Javalin;

import java.time.LocalTime;

public class LogisticaApp {

  private static final int PUERTO = 8081;
  private static final LocalTime HORARIO_POR_DEFECTO = LocalTime.of(3, 0);

  public static void main(String[] args) {
    String urlBase = variable("LOGISTICA_URL_BASE", "http://localhost:" + PUERTO);
    String urlDonaciones = variable("DONACIONES_URL_BASE", "http://localhost:8080");
    String urlPlanificador = variable("PLANIFICADOR_URL", "http://localhost:9090/planificaciones");
    String tokenPlanificador = variable("PLANIFICADOR_TOKEN", "token-de-desarrollo");
    boolean simulado = !"http".equalsIgnoreCase(variable("LOGISTICA_MODO", "simulado"));

    SerializadorJson json = new SerializadorJackson();

    ClienteDonaciones clienteDonaciones = simulado
        ? new ClienteDonacionesSimulado()
        : new ClienteDonacionesHttp(urlDonaciones, json);

    GeneradorRutas generadorRutas = simulado
        ? new GeneradorRutasSimulado()
        : new GeneradorRutasExterno(urlPlanificador, json);

    GestorCamiones gestorCamiones = new GestorCamiones();
    GestorRutas gestorRutas = new GestorRutas();
    GestorEntregas gestorEntregas = new GestorEntregas(clienteDonaciones, urlBase);
    GestorMonitoreo gestorMonitoreo = new GestorMonitoreo();
    GestorLogistica gestorLogistica = new GestorLogistica(
        generadorRutas,
        clienteDonaciones,
        urlBase + "/planificacion/callback"
    );

    Javalin app = Javalin.create().start(PUERTO);

    new CamionesController(gestorCamiones).registrarRutas(app);
    new RutasController(gestorRutas, gestorEntregas).registrarRutas(app);
    new EntregasController(gestorEntregas).registrarRutas(app);
    new MonitoreoController(gestorMonitoreo, gestorRutas).registrarRutas(app);
    new PlanificacionController(gestorLogistica).registrarRutas(app);
    new RutasCallbackController(gestorLogistica, tokenPlanificador).registrarRutas(app);

    registrarManejoDeErrores(app);

    JobPlanificacionRutas job = new JobPlanificacionRutas(
        gestorLogistica,
        JobPlanificacionRutas.horarioDesde(System.getenv("PLANIFICACION_HORA"), HORARIO_POR_DEFECTO)
    );
    job.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(job::detener));

    System.out.println("Microservicio Logistica escuchando en " + urlBase
        + " | modo: " + (simulado ? "simulado" : "http"));
  }

  private static void registrarManejoDeErrores(Javalin app) {
    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));

    app.exception(IllegalArgumentException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));

    app.exception(IllegalStateException.class, (e, ctx) ->
        ctx.status(409).json(new ErrorDTO(e.getMessage())));

    app.exception(IntegracionExternaException.class, (e, ctx) ->
        ctx.status(502).json(new ErrorDTO(e.getMessage())));

    app.exception(UnsupportedOperationException.class, (e, ctx) ->
        ctx.status(501).json(new ErrorDTO(e.getMessage())));
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }
}
