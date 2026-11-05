package donatrack.web;

import donatrack.web.controller.DonacionesWebController;
import donatrack.web.controller.LogisticaWebController;
import donatrack.web.controller.PublicoController;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.ClienteLogisticaHttp;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.IdentidadPorQueryString;
import donatrack.web.templates.JavalinHandlebars;
import donatrack.web.templates.JavalinRenderer;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class WebApp {

  private static final int PUERTO = 8082;

  public static void main(String[] args) {
    String urlLogistica = variable("LOGISTICA_URL_BASE", "http://localhost:8081");

    ClienteLogistica clienteLogistica = new ClienteLogisticaHttp(urlLogistica);
    IdentidadDelUsuario identidad = new IdentidadPorQueryString();

    JavalinRenderer renderer = new JavalinRenderer()
        .register("hbs", new JavalinHandlebars());

    Javalin app = Javalin.create(config -> {
      config.fileRenderer(renderer);
      config.staticFiles.add("/public", Location.CLASSPATH);
    }).start(PUERTO);

    new PublicoController().registrarRutas(app);
    new DonacionesWebController().registrarRutas(app);
    new LogisticaWebController(clienteLogistica, identidad).registrarRutas(app);

    System.out.println("Cliente web de DonaTrack escuchando en http://localhost:" + PUERTO
        + " | Logistica: " + urlLogistica);
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }
}
