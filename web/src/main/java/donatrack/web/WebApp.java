package donatrack.web;

import donatrack.web.controller.DonacionesWebController;
import donatrack.web.controller.LogisticaWebController;
import donatrack.web.controller.PublicoController;
import donatrack.web.templates.JavalinHandlebars;
import donatrack.web.templates.JavalinRenderer;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class WebApp {

  private static final int PUERTO = 8082;

  public static void main(String[] args) {
    JavalinRenderer renderer = new JavalinRenderer()
        .register("hbs", new JavalinHandlebars());

    Javalin app = Javalin.create(config -> {
      config.fileRenderer(renderer);
      config.staticFiles.add("/public", Location.CLASSPATH);
    }).start(PUERTO);

    new PublicoController().registrarRutas(app);
    new DonacionesWebController().registrarRutas(app);
    new LogisticaWebController().registrarRutas(app);

    System.out.println("Cliente web de DonaTrack escuchando en http://localhost:" + PUERTO);
  }
}
