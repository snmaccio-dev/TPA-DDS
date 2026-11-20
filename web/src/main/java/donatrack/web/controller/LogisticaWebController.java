package donatrack.web.controller;

import donatrack.web.Alertas;
import donatrack.web.Modelo;
import donatrack.web.integracion.ArchivoSubido;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.IntegracionException;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.integracion.ServicioNoDisponibleException;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.vista.PlanificacionVista;
import donatrack.web.vista.RutaVista;
import donatrack.web.vista.SeguimientoVista;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LogisticaWebController {

  private static final String TIPO_POR_DEFECTO = "application/octet-stream";

  private final ClienteLogistica clienteLogistica;
  private final IdentidadDelUsuario identidad;

  // URL de Logistica que resuelve el navegador. LOGISTICA_URL_BASE, en Docker, es la
  // interna (http://logistica:8081) y el navegador no la alcanza.
  private final String urlLogisticaPublica;

  public LogisticaWebController(
      ClienteLogistica clienteLogistica,
      IdentidadDelUsuario identidad,
      String urlLogisticaPublica
  ) {
    this.clienteLogistica = clienteLogistica;
    this.identidad = identidad;
    this.urlLogisticaPublica = urlLogisticaPublica;
  }

  public void registrarRutas(Javalin app) {
    app.get("/camiones", this::camiones);

    app.get("/monitoreo-flota", this::monitoreoDeFlota);
    app.get("/rutas-planificadas", this::rutasPlanificadas);

    app.get("/chofer-inicio-ruta", this::rutasDelChofer);
    app.post("/chofer-inicio-ruta/{id}", this::iniciarRuta);

    app.get("/seguimiento-donante", this::seguimientoDonante);
    app.get("/seguimiento-beneficiario", this::seguimientoBeneficiaria);

    app.get("/confirmar-recepcion", this::confirmarRecepcion);
    app.post("/confirmar-recepcion/{id}", this::registrarRecepcion);
  }

  // La administracion de camiones vive en Logistica, con su propio login.
  private void camiones(Context ctx) {
    ctx.redirect(urlLogisticaPublica + "/admin/camiones");
  }

  private void monitoreoDeFlota(Context ctx) {
    ctx.render("monitoreo-flota.html.hbs",
        modeloBase(ctx, "Monitoreo de flota", "Monitoreo")
            .comoAdmin().activa("monitoreo").conMapa()
            .conDatosDe("seguimiento", SeguimientoVista.VACIO, () -> seguimiento(ctx))
            .mapa());
  }

  private void rutasPlanificadas(Context ctx) {
    ctx.render("rutas-planificadas.html.hbs",
        modeloBase(ctx, "Rutas planificadas", "Planificación")
            .comoAdmin().activa("rutasPlanificadas")
            .conDatosDe("planificacion", PlanificacionVista.VACIA, () -> planificacion(ctx))
            .mapa());
  }

  private PlanificacionVista planificacion(Context ctx) {
    return clienteLogistica.planificacion(autorizacionDe(ctx));
  }

  private void rutasDelChofer(Context ctx) {
    mostrarRutasDelChofer(ctx, null);
  }

  private void iniciarRuta(Context ctx) {
    try {
      clienteLogistica.iniciarRuta(
          autorizacionDe(ctx),
          ctx.pathParamAsClass("id", Long.class).get()
      );
    } catch (IntegracionException e) {
      Alertas.flashError(ctx, mensajeDe(e));
    }

    ctx.redirect("/chofer-inicio-ruta");
  }

  private void mostrarRutasDelChofer(Context ctx, String alertaError) {
    Modelo modelo = modeloBase(ctx, "Mis rutas", "Reparto")
        .comoChofer().activa("misRutas")
        .conAlertaError(alertaError)
        .conDatosDe("rutas", List.<RutaVista>of(),
            () -> clienteLogistica.rutasDelChofer(autorizacionDe(ctx)));

    Object rutas = modelo.mapa().get("rutas");
    modelo.con("sinRutas", rutas instanceof List<?> lista && lista.isEmpty());

    ctx.render("chofer-inicio-ruta.html.hbs", modelo.mapa());
  }

  private void seguimientoDonante(Context ctx) {
    ctx.render("seguimiento-donante.html.hbs",
        modeloBase(ctx, "Seguimiento", "Seguimiento")
            .comoDonante().activa("seguimiento").conMapa()
            .conDatosDe("seguimiento", SeguimientoVista.VACIO, () -> seguimiento(ctx))
            .mapa());
  }

  private void seguimientoBeneficiaria(Context ctx) {
    ctx.render("seguimiento-beneficiario.html.hbs",
        modeloBase(ctx, "Seguimiento", "Seguimiento")
            .comoBeneficiaria().activa("seguimiento").conMapa()
            .conDatosDe("seguimiento", SeguimientoVista.VACIO, () -> seguimiento(ctx))
            .mapa());
  }

  private void confirmarRecepcion(Context ctx) {
    mostrarConfirmacion(ctx, entregaPedida(ctx), null);
  }

  private void registrarRecepcion(Context ctx) {
    String autorizacion = autorizacionDe(ctx);
    long entregaId = ctx.pathParamAsClass("id", Long.class).get();

    List<ArchivoSubido> fotos;
    try {
      fotos = fotosDe(ctx);
    } catch (OperacionRechazadaException e) {
      mostrarConfirmacion(ctx, entregaId, e.getMessage());
      return;
    }

    try {
      clienteLogistica.confirmarRecepcion(
          autorizacion,
          entregaId,
          clienteLogistica.subirFotos(autorizacion, entregaId, fotos)
      );
    } catch (IntegracionException e) {
      mostrarConfirmacion(ctx, entregaId, mensajeDe(e));
      return;
    }

    ctx.redirect("/confirmar-recepcion?entrega=" + entregaId);
  }

  private List<ArchivoSubido> fotosDe(Context ctx) {
    List<ArchivoSubido> fotos = ctx.uploadedFiles("fotos").stream()
        .map(this::comoArchivo)
        .toList();

    if (fotos.isEmpty()) {
      throw new OperacionRechazadaException(
          "Tenés que adjuntar al menos una fotografía como comprobante."
      );
    }

    return fotos;
  }

  private ArchivoSubido comoArchivo(UploadedFile archivo) {
    try (InputStream contenido = archivo.content()) {
      return new ArchivoSubido(
          archivo.filename(),
          archivo.contentType() == null ? TIPO_POR_DEFECTO : archivo.contentType(),
          contenido.readAllBytes()
      );
    } catch (IOException e) {
      throw new OperacionRechazadaException(
          "No se pudo leer el archivo " + archivo.filename() + "."
      );
    }
  }

  private void mostrarConfirmacion(Context ctx, Long entregaId, String alertaError) {
    Modelo modelo = modeloBase(ctx, "Confirmar recepción", "Confirmar recepción")
        .comoBeneficiaria().activa("confirmar")
        .conAlertaError(alertaError);

    if (entregaId == null) {
      modelo.con("faltaLaEntrega", true);
    } else {
      modelo.con("entregaId", entregaId)
          .conDatosDe("entrega", null,
              () -> clienteLogistica.entrega(autorizacionDe(ctx), entregaId));
    }

    ctx.render("confirmar-recepcion.html.hbs", modelo.mapa());
  }

  private Modelo modeloBase(Context ctx, String titulo, String seccion) {
    return Modelo.de(titulo).seccion(seccion).conUsuario(usuario(ctx).perfil());
  }

  private SeguimientoVista seguimiento(Context ctx) {
    return new SeguimientoVista(
        clienteLogistica.monitoreoDeRutasEnCurso(autorizacionDe(ctx))
    );
  }

  private Long entregaPedida(Context ctx) {
    String valor = ctx.queryParam("entrega");

    if (valor == null || valor.isBlank()) {
      return null;
    }

    try {
      return Long.parseLong(valor.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String autorizacionDe(Context ctx) {
    return usuario(ctx).autorizacion();
  }

  private UsuarioWeb usuario(Context ctx) {
    return identidad.actual(ctx).orElseThrow(() ->
        new InternalServerErrorResponse("No hay una sesion iniciada."));
  }

  static String mensajeDe(IntegracionException e) {
    if (e instanceof ServicioNoDisponibleException) {
      return Alertas.SERVICIO_NO_DISPONIBLE;
    }
    return e.getMessage();
  }
}
