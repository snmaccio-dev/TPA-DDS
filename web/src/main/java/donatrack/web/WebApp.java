package donatrack.web;

import donatrack.web.controller.DonacionesWebController;
import donatrack.web.controller.LogisticaWebController;
import donatrack.web.controller.PublicoController;
import donatrack.web.controller.SesionController;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.ClienteDonacionesHttp;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.ClienteLogisticaHttp;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.sesion.ClienteOidc;
import donatrack.web.sesion.ConfiguracionOidc;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.IdentidadPorSesion;
import donatrack.web.sesion.IdentidadSimulada;
import donatrack.web.sesion.SesionExpiradaException;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.templates.JavalinHandlebars;
import donatrack.web.templates.JavalinRenderer;
import io.javalin.Javalin;
import io.javalin.http.Context;
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
      "/sesion/ingresar-admin",
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
      if (esPublica(ctx.path())) {
        return;
      }

      UsuarioWeb usuario = identidad.actual(ctx).orElse(null);

      if (usuario == null) {
        ctx.redirect("/login");
        ctx.skipRemainingHandlers();
        return;
      }

      if (usuario.estaPorVencer() && usuario.sePuedeRenovar()) {
        try {
          ctx.sessionAttribute(
              IdentidadPorSesion.CLAVE_USUARIO,
              usuario.conTokens(oidc.renovar(usuario.refreshToken()))
          );
        } catch (RuntimeException e) {
          throw new SesionExpiradaException("La sesión expiró. Volvé a ingresar.");
        }
      }

      String rolRequerido = rolRequerido(ctx.path());

      if (rolRequerido != null && !tieneRol(usuario, rolRequerido)) {
        ctx.status(403);
        ctx.render(
            "acceso-denegado.html.hbs",
            Modelo.de("Acceso denegado").mapa()
        );
        ctx.skipRemainingHandlers();
      }
    });


    app.exception(SesionExpiradaException.class, (e, ctx) -> {
      ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO, null);
      Alertas.flashAdvertencia(ctx, "Tu sesión expiró, volvé a ingresar.");
      ctx.redirect("/login");
    });

    app.exception(OperacionRechazadaException.class, (e, ctx) -> {
      if (e.esProhibido()) {
        redirigirConAlerta(ctx, identidad, Alertas.TIPO_ERROR,
            "No tenés permisos para esa acción.");
      } else {
        redirigirConAlerta(ctx, identidad, Alertas.TIPO_ERROR, e.getMessage());
      }
    });

    app.exception(Exception.class, (e, ctx) -> {
      // Logueamos al stderr para que quede en el historial del servidor, sin exponer el
      // stack trace al usuario.
      e.printStackTrace();
      redirigirConAlerta(ctx, identidad, Alertas.TIPO_ERROR,
          "Ocurrió un error inesperado.");
    });

    app.error(404, ctx -> redirigirConAlerta(ctx, identidad, Alertas.TIPO_ERROR,
        "La página que buscás no existe."));

    new PublicoController(clienteDonaciones, clienteLogistica).registrarRutas(app);
    new SesionController(oidc, clienteDonaciones).registrarRutas(app);
    new DonacionesWebController(clienteDonaciones, identidad).registrarRutas(app);
    new LogisticaWebController(clienteLogistica, identidad, urlLogistica).registrarRutas(app);

    System.out.println("Cliente web de DonaTrack escuchando en " + urlBase
        + " | Logistica: " + urlLogistica
        + " | Donaciones: " + urlDonaciones
        + " | seguridad: " + (seguridadActiva ? "on" : "off"));
  }

  // Mandamos al usuario a la pagina anterior (Referer) o, si no hay, al inicio que le
  // corresponde por rol (o "/" si no tiene sesion). Si el destino del redirect es la
  // misma ruta que fallo, caemos a "/" para no entrar en un bucle.
  private static void redirigirConAlerta(Context ctx, IdentidadDelUsuario identidad,
                                         String tipo, String texto) {
    Alertas.flash(ctx, tipo, texto);
    String destino = destinoSeguro(ctx, identidad);
    ctx.redirect(destino);
  }

  private static String destinoSeguro(Context ctx, IdentidadDelUsuario identidad) {
    String referer = ctx.header("Referer");
    String actual = ctx.path();

    if (referer != null && !referer.isBlank() && !mismaRuta(referer, actual)) {
      return referer;
    }

    UsuarioWeb usuario = identidad.actual(ctx).orElse(null);
    String inicio = usuario == null || usuario.perfil() == null
        ? "/"
        : usuario.perfil().inicioSegunRol();

    if (mismaRuta(inicio, actual)) {
      return "/";
    }
    return inicio;
  }

  private static boolean mismaRuta(String referencia, String path) {
    if (referencia == null || path == null) return false;
    // El Referer viene como URL absoluta; alcanza con comparar el final del path.
    return referencia.endsWith(path);
  }

  private static boolean esPublica(String ruta) {
    return RUTAS_PUBLICAS.contains(ruta)
        || ruta.startsWith("/assets/")
        || "/favicon.ico".equals(ruta);
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }

  // Nueva funcion static 

  private static String rolRequerido(String ruta) {

    // DONANTE
    if (Set.of(
        "/donante-dashboard",
        "/mis-donaciones",
        "/entidades",
        "/donaciones/nueva",
        "/seguimiento-donante"
    ).contains(ruta)) {
      return "DONANTE";
    }

    // BENEFICIARIA
    if (Set.of(
        "/beneficiario-dashboard",
        "/necesidades",
        "/donaciones-asignadas",
        "/seguimiento-beneficiario",
        "/confirmar-recepcion"
    ).contains(ruta)) {
      return "BENEFICIARIA";
    }

    // ADMINISTRADOR
    if (Set.of(
        "/admin-dashboard",
        "/admin-donantes",
        "/deposito",
        "/asignaciones",
        "/importar-csv",
        "/camiones",
        "/monitoreo-flota",
        "/rutas-planificadas"
    ).contains(ruta)
        || ruta.startsWith("/deposito/")
        || ruta.startsWith("/asignaciones/")
        || ruta.startsWith("/camiones/")) {
      return "ADMINISTRADOR";
    }

    // CHOFER
    if (ruta.equals("/chofer-inicio-ruta")) {
      return "CHOFER";
    }

    // Notificaciones: cualquier usuario autenticado.
    return null;
  }

  private static boolean tieneRol(UsuarioWeb usuario, String rol) {
    if (usuario.perfil() == null) {
      return false;
    }

    return switch (rol) {
      case "DONANTE" -> usuario.perfil().isDonante();
      case "BENEFICIARIA" -> usuario.perfil().isBeneficiaria();
      case "ADMINISTRADOR" -> usuario.perfil().isAdministrador();
      case "CHOFER" -> usuario.perfil().isChofer();
      default -> false;
    };
  }

}
