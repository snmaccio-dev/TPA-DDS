package donatrack.logistica;

import donatrack.logistica.controller.CamionesController;
import donatrack.logistica.controller.ChoferesController;
import donatrack.logistica.controller.CamionesWebController;
import donatrack.logistica.controller.EntregasController;
import donatrack.logistica.controller.MonitoreoController;
import donatrack.logistica.controller.PlanificacionController;
import donatrack.logistica.controller.RutasCallbackController;
import donatrack.logistica.controller.RutasController;
import donatrack.logistica.controller.SesionCamionesController;
import donatrack.logistica.controller.dto.ErrorDTO;
import donatrack.logistica.domain.ConflictoDeNegocioException;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.TransicionNoPermitidaException;
import donatrack.logistica.domain.almacenamiento.AlmacenDeFotos;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;
import donatrack.logistica.domain.planificacion.GeneradorRutas;
import donatrack.logistica.infrastructure.almacenamiento.AlmacenEnDisco;
import donatrack.logistica.infrastructure.integracion.ClienteDonacionesHttp;
import donatrack.logistica.infrastructure.integracion.ClienteDonacionesSimulado;
import donatrack.logistica.infrastructure.integracion.DirectorioDeUsuariosHttp;
import donatrack.logistica.infrastructure.integracion.ProveedorDeTokenDeServicio;
import donatrack.logistica.infrastructure.integracion.SerializadorJackson;
import donatrack.logistica.infrastructure.planificacion.GeneradorRutasExterno;
import donatrack.logistica.infrastructure.planificacion.GeneradorRutasSimulado;
import donatrack.logistica.repository.RepositorioRutas;
import donatrack.logistica.graphql.EsquemaGraphQl;
import donatrack.logistica.graphql.GraphQlController;
import donatrack.logistica.jobs.JobPlanificacionRutas;
import donatrack.logistica.seguridad.ControlDeAcceso;
import donatrack.logistica.seguridad.ControlDeAccesoAbierto;
import donatrack.logistica.seguridad.ControlDeAccesoPorToken;
import donatrack.logistica.seguridad.FiltroDeAcceso;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.seguridad.VerificadorDeTokens;
import donatrack.logistica.seguridad.web.ClienteOidc;
import donatrack.logistica.seguridad.web.ConfiguracionOidc;
import donatrack.logistica.service.GestorCamiones;
import donatrack.logistica.service.GestorChoferes;
import donatrack.logistica.service.GestorEntregas;
import donatrack.logistica.service.GestorLogistica;
import donatrack.logistica.service.GestorMonitoreo;
import donatrack.logistica.service.GestorRutas;
import donatrack.logistica.service.RecursoInexistenteException;
import donatrack.logistica.templates.JavalinHandlebars;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.session.SessionHandler;

import java.time.LocalTime;

public class LogisticaApp {

  private static final int PUERTO = 8081;
  private static final LocalTime HORARIO_POR_DEFECTO = LocalTime.of(3, 0);

  public static void main(String[] args) {
    String urlBase = variable("LOGISTICA_URL_BASE", "http://localhost:" + PUERTO);
    String urlDonaciones = variable("DONACIONES_URL_BASE", "http://localhost:8080");
    String urlWeb = variable("WEB_URL_BASE", "http://localhost:8082");
    String urlPlanificador = variable("PLANIFICADOR_URL", "http://localhost:9090/planificaciones");
    String tokenPlanificador = variable("PLANIFICADOR_TOKEN", "token-de-desarrollo");
    boolean donacionesSimuladas =
        !"http".equalsIgnoreCase(variable("LOGISTICA_MODO", "simulado"));

    // El planificador de rutas es un servicio de terceros y vive aparte del modo de
    // integracion con Donaciones: se puede consumir Donaciones por HTTP y seguir
    // generando rutas localmente, que es la situacion mientras no haya un
    // planificador desplegado.
    boolean planificadorSimulado =
        !"http".equalsIgnoreCase(variable("LOGISTICA_PLANIFICADOR", "simulado"));
    boolean seguridadActiva = !"off".equalsIgnoreCase(variable("LOGISTICA_SEGURIDAD", "on"));

    String keycloakUrl = variable("KEYCLOAK_URL", "http://localhost:8180");
    String keycloakRealm = variable("KEYCLOAK_REALM", "donatrack");

    String directorioFotos = variable("LOGISTICA_FOTOS_DIR", "fotos");

    SerializadorJson json = new SerializadorJackson();

    AlmacenDeFotos almacenDeFotos = new AlmacenEnDisco(directorioFotos, urlBase + "/fotos");

    ClienteDonaciones clienteDonaciones;
    if (donacionesSimuladas) {
      clienteDonaciones = ClienteDonacionesSimulado.conDonacionesDeEjemplo();
    } else {
      // Keycloak. El client secret no tiene default: si falta el arranque falla
      // porque sin token no se puede hablar con Donaciones.
      String keycloakClientId = variable("KEYCLOAK_CLIENT_ID", "logistica-backend");
      String keycloakClientSecret = variable("KEYCLOAK_CLIENT_SECRET", null);
      if (keycloakClientSecret == null || keycloakClientSecret.isBlank()) {
        throw new IllegalStateException(
            "Falta la variable KEYCLOAK_CLIENT_SECRET. Sin ella Logistica "
                + "no puede obtener el token de servicio para llamar a Donaciones."
        );
      }
      ProveedorDeTokenDeServicio proveedorToken = new ProveedorDeTokenDeServicio(
          keycloakUrl, keycloakRealm, keycloakClientId, keycloakClientSecret);
      clienteDonaciones = new ClienteDonacionesHttp(urlDonaciones, json, proveedorToken);
    }

    // El simulado responde solo al callback, con el mismo token que el proveedor externo.
    GeneradorRutas generadorRutas = planificadorSimulado
        ? new GeneradorRutasSimulado(tokenPlanificador, json,
            RepositorioRutas.getInstance()::patentesConRutaPlanificadaOEnCurso)
        : new GeneradorRutasExterno(urlPlanificador, json);

    GestorCamiones gestorCamiones = new GestorCamiones();
    GestorChoferes gestorChoferes = new GestorChoferes();
    GestorRutas gestorRutas = new GestorRutas();
    GestorEntregas gestorEntregas =
        new GestorEntregas(clienteDonaciones, urlBase, almacenDeFotos);
    GestorMonitoreo gestorMonitoreo = new GestorMonitoreo();
    GestorLogistica gestorLogistica = new GestorLogistica(
        generadorRutas,
        clienteDonaciones,
        urlBase + "/planificacion/callback"
    );

    Javalin app = Javalin.create(config -> {
      config.fileRenderer(new JavalinHandlebars());
      config.staticFiles.add("/public", Location.CLASSPATH);
      config.staticFiles.add(estaticos -> {
        estaticos.directory = directorioFotos;
        estaticos.location = Location.EXTERNAL;
        estaticos.hostedPath = "/fotos";
      });
      // Web tambien usa JSESSIONID y en localhost las cookies no distinguen puerto: con
      // el mismo nombre una sesion pisaria a la otra. Lax permite que la cookie viaje en
      // la redireccion de vuelta desde Keycloak al callback.
      config.jetty.modifyServletContextHandler(contexto -> {
        SessionHandler sesiones = contexto.getSessionHandler();
        sesiones.setSessionCookie("LOGISTICA_SESSION");
        sesiones.setHttpOnly(true);
        sesiones.setSameSite(HttpCookie.SameSite.LAX);
      });
    }).start(PUERTO);

    app.after(ctx -> WithSimplePersistenceUnit.dispose());

    /*
    ControlDeAcceso control;
    if (seguridadActiva) {
      new FiltroDeAcceso(new VerificadorDeTokens(
          keycloakUrl,
          keycloakRealm,
          variable("KEYCLOAK_ISSUER", null)
      )).registrarEn(app);

      control = new ControlDeAccesoPorToken(
          new DirectorioDeUsuariosHttp(urlDonaciones, json)
      );
    } else {
      control = new ControlDeAccesoAbierto();
    }
    */

    // Se reemplaza esta parte por esto 

    VerificadorDeTokens verificador = seguridadActiva
        ? new VerificadorDeTokens(
            keycloakUrl,
            keycloakRealm,
            variable("KEYCLOAK_ISSUER", null)
        )
        : null;

    ControlDeAcceso control;

    if (seguridadActiva) {
        new FiltroDeAcceso(verificador).registrarEn(app);

        control = new ControlDeAccesoPorToken(
            new DirectorioDeUsuariosHttp(urlDonaciones, json)
        );
    } else {
        control = new ControlDeAccesoAbierto();
    }

    // Las rutas de sesion van antes que las de la pantalla: POST /admin/camiones/salir
    // tambien coincide con POST /admin/camiones/{patente}.
    if (seguridadActiva) {
      ClienteOidc oidc = new ClienteOidc(new ConfiguracionOidc(
          keycloakUrl,
          variable("KEYCLOAK_URL_PUBLICA", keycloakUrl),
          keycloakRealm,
          variable("KEYCLOAK_WEB_CLIENT_ID", "logistica-web"),
          urlBase,
          urlWeb
      ));
      new SesionCamionesController(oidc, verificador, urlWeb).registrarRutas(app);
    } else {
      // Sin seguridad no hay login: salir solo vuelve a web.
      app.post("/admin/camiones/salir", ctx -> ctx.redirect(urlWeb), RolDeAcceso.PUBLICO);
    }

    new CamionesWebController(gestorCamiones, urlWeb).registrarRutas(app);

    new CamionesController(gestorCamiones).registrarRutas(app);
    
    new ChoferesController(gestorChoferes).registrarRutas(app);
    new RutasController(gestorRutas, gestorEntregas, gestorChoferes, control)
        .registrarRutas(app);
    new EntregasController(gestorEntregas, control).registrarRutas(app);
    new MonitoreoController(gestorMonitoreo, gestorRutas, control).registrarRutas(app);
    new PlanificacionController(gestorLogistica).registrarRutas(app);
    new RutasCallbackController(gestorLogistica, tokenPlanificador).registrarRutas(app);

    new GraphQlController(EsquemaGraphQl.construir(gestorRutas, gestorLogistica))
        .registrarRutas(app);

    registrarManejoDeErrores(app);

    JobPlanificacionRutas job = new JobPlanificacionRutas(
        gestorLogistica,
        JobPlanificacionRutas.horarioDesde(System.getenv("PLANIFICACION_HORA"), HORARIO_POR_DEFECTO)
    );
    job.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(job::detener));

    System.out.println("Microservicio Logistica escuchando en " + urlBase
        + " | donaciones: " + (donacionesSimuladas ? "simulado" : "http")
        + " | planificador: " + (planificadorSimulado ? "simulado" : "http")
        + " | seguridad: " + (seguridadActiva ? "on" : "off"));
  }

  private static void registrarManejoDeErrores(Javalin app) {
    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));

    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));

    app.exception(ConflictoDeNegocioException.class, (e, ctx) ->
        ctx.status(409).json(new ErrorDTO(e.getMessage())));

    app.exception(TransicionNoPermitidaException.class, (e, ctx) ->
        ctx.status(422).json(new ErrorDTO(e.getMessage())));

    app.exception(IntegracionExternaException.class, (e, ctx) ->
        ctx.status(502).json(new ErrorDTO(e.getMessage())));

    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));

    app.exception(Exception.class, (e, ctx) -> {
      e.printStackTrace();
      ctx.status(500).json(new ErrorDTO("Error interno del servicio de Logistica."));
    });
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }
}
