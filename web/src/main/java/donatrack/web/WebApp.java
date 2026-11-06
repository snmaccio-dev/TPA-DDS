package donatrack.web;

import donatrack.web.controller.DonacionesWebController;
import donatrack.web.controller.LogisticaWebController;
import donatrack.web.controller.PublicoController;
import donatrack.web.controller.SesionController;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.ClienteDonacionesHttp;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.ClienteLogisticaHttp;
import donatrack.web.sesion.ClienteOidc;
import donatrack.web.sesion.ConfiguracionOidc;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.IdentidadPorSesion;
import donatrack.web.sesion.IdentidadSimulada;
import donatrack.web.templates.JavalinHandlebars;
import donatrack.web.templates.JavalinRenderer;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.Set;

public class WebApp {

  private static final int PUERTO = 8082;

  private static final Set<String> RUTAS_PUBLICAS = Set.of(
      "/",
      "/donaciones-publicas",
      "/registro",
      "/login",
      "/privacidad",
      "/sesion/ingresar",
      "/callback",
      "/logout"
  );

  public static void main(String[] args) {
    String urlBase = variable("WEB_URL_BASE", "http://localhost:" + PUERTO);
    String urlLogistica = variable("LOGISTICA_URL_BASE", "http://localhost:8081");
    String urlDonaciones = variable("DONACIONES_URL_BASE", "http://localhost:8080");
    boolean seguridadActiva = !"off".equalsIgnoreCase(variable("WEB_SEGURIDAD", "on"));

    ClienteLogistica clienteLogistica = new ClienteLogisticaHttp(urlLogistica);
    ClienteDonaciones clienteDonaciones = new ClienteDonacionesHttp(urlDonaciones);

    ClienteOidc oidc = new ClienteOidc(new ConfiguracionOidc(
        variable("KEYCLOAK_URL", "http://localhost:8180"),
        variable("KEYCLOAK_REALM", "donatrack"),
        variable("KEYCLOAK_CLIENT_ID", "donatrack-web"),
        urlBase
    ));

    IdentidadDelUsuario identidad = seguridadActiva
        ? new IdentidadPorSesion()
        : new IdentidadSimulada();

    JavalinRenderer renderer = new JavalinRenderer()
        .register("hbs", new JavalinHandlebars());

    Javalin app = Javalin.create(config -> {
      config.fileRenderer(renderer);
      config.staticFiles.add("/public", Location.CLASSPATH);
    }).start(PUERTO);

    app.beforeMatched(ctx -> {
      if (esPublica(ctx.path()) || identidad.actual(ctx).isPresent()) {
        return;
      }

      ctx.redirect("/login");
      ctx.skipRemainingHandlers();
    });

    new PublicoController().registrarRutas(app);
    new SesionController(oidc, clienteDonaciones).registrarRutas(app);
    new DonacionesWebController().registrarRutas(app);
    new LogisticaWebController(clienteLogistica, identidad).registrarRutas(app);

    System.out.println("Cliente web de DonaTrack escuchando en " + urlBase
        + " | Logistica: " + urlLogistica
        + " | Donaciones: " + urlDonaciones
        + " | seguridad: " + (seguridadActiva ? "on" : "off"));
  }

  // Los estaticos tienen que quedar afuera del filtro: si no, el CSS y el JS se van
  // redirigidos al login y las pantallas se ven sin estilos.
  private static boolean esPublica(String ruta) {
    return RUTAS_PUBLICAS.contains(ruta)
        || ruta.startsWith("/assets/")
        || "/favicon.ico".equals(ruta);
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }
}
