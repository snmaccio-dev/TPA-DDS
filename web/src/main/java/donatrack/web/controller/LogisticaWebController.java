package donatrack.web.controller;

import donatrack.web.Modelo;
import donatrack.web.integracion.ArchivoSubido;
import donatrack.web.integracion.CapacidadesDeCamion;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.NuevoCamion;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.vista.FlotaVista;
import donatrack.web.vista.SeguimientoVista;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LogisticaWebController {

  private static final String TIPO_POR_DEFECTO = "application/octet-stream";

  private final ClienteLogistica clienteLogistica;
  private final IdentidadDelUsuario identidad;

  public LogisticaWebController(ClienteLogistica clienteLogistica,
                                IdentidadDelUsuario identidad) {
    this.clienteLogistica = clienteLogistica;
    this.identidad = identidad;
  }

  public void registrarRutas(Javalin app) {
    app.get("/camiones", this::camiones);
    app.post("/camiones", this::crearCamion);
    app.get("/camiones/{patente}/editar", this::formularioDeEdicion);
    app.post("/camiones/{patente}", this::actualizarCamion);

    app.get("/seguimiento-donante", this::seguimientoDonante);
    app.get("/seguimiento-beneficiario", this::seguimientoBeneficiaria);

    app.get("/confirmar-recepcion", this::confirmarRecepcion);
    app.post("/confirmar-recepcion/{id}", this::registrarRecepcion);
  }

  private void camiones(Context ctx) {
    mostrarCamiones(ctx, null);
  }

  private void crearCamion(Context ctx) {
    NuevoCamion nuevo = new NuevoCamion(
        ctx.formParam("patente"),
        numero(ctx, "capacidadVolumen"),
        numero(ctx, "altura"),
        numero(ctx, "capacidadCarga")
    );

    try {
      clienteLogistica.crearCamion(nuevo);
    } catch (OperacionRechazadaException e) {
      mostrarCamiones(ctx, e.getMessage());
      return;
    }

    ctx.redirect("/camiones");
  }

  private void formularioDeEdicion(Context ctx) {
    mostrarEdicion(ctx, ctx.pathParam("patente"), null);
  }

  private void actualizarCamion(Context ctx) {
    String patente = ctx.pathParam("patente");

    CapacidadesDeCamion capacidades = new CapacidadesDeCamion(
        numero(ctx, "capacidadVolumen"),
        numero(ctx, "altura"),
        numero(ctx, "capacidadCarga")
    );

    try {
      clienteLogistica.actualizarCamion(patente, capacidades);
    } catch (OperacionRechazadaException e) {
      mostrarEdicion(ctx, patente, e.getMessage());
      return;
    }

    ctx.redirect("/camiones");
  }

  private void seguimientoDonante(Context ctx) {
    List<Long> donaciones = identidad.donaciones(ctx).orElse(null);

    Modelo modelo = Modelo.de("Seguimiento").seccion("Seguimiento")
        .comoDonante().activa("seguimiento").conMapa()
        .con("seguimiento", SeguimientoVista.VACIO);

    if (donaciones == null) {
      sinIdentidad(modelo);
    } else {
      modelo.conDatosDe("seguimiento", SeguimientoVista.VACIO,
          () -> new SeguimientoVista(clienteLogistica.monitoreoDeDonaciones(donaciones)));
    }

    ctx.render("seguimiento-donante.html.hbs", modelo.mapa());
  }

  private void seguimientoBeneficiaria(Context ctx) {
    Long beneficiaria = identidad.beneficiaria(ctx).orElse(null);

    Modelo modelo = Modelo.de("Seguimiento").seccion("Seguimiento")
        .comoBeneficiaria().activa("seguimiento").conMapa()
        .con("seguimiento", SeguimientoVista.VACIO);

    if (beneficiaria == null) {
      sinIdentidad(modelo);
    } else {
      modelo.conDatosDe("seguimiento", SeguimientoVista.VACIO,
          () -> new SeguimientoVista(clienteLogistica.monitoreoDeBeneficiaria(beneficiaria)));
    }

    ctx.render("seguimiento-beneficiario.html.hbs", modelo.mapa());
  }

  private void confirmarRecepcion(Context ctx) {
    mostrarConfirmacion(ctx, identidad.entregaAConfirmar(ctx).orElse(null), null);
  }

  private void registrarRecepcion(Context ctx) {
    long entregaId = ctx.pathParamAsClass("id", Long.class).get();

    try {
      clienteLogistica.confirmarRecepcion(
          entregaId,
          clienteLogistica.subirFotos(entregaId, fotosDe(ctx))
      );
    } catch (OperacionRechazadaException e) {
      mostrarConfirmacion(ctx, entregaId, e.getMessage());
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

  private void mostrarCamiones(Context ctx, String errorDelFormulario) {
    ctx.render("camiones.html.hbs",
        Modelo.de("Camiones").seccion("Camiones").comoAdmin().activa("camiones")
            .conDatosDe("flota", FlotaVista.VACIA, this::flota)
            .con("errorDelFormulario", errorDelFormulario)
            .mapa());
  }

  private void mostrarEdicion(Context ctx, String patente, String errorDelFormulario) {
    ctx.render("camion-editar.html.hbs",
        Modelo.de("Editar camión").seccion("Camiones").comoAdmin().activa("camiones")
            .conDatosDe("camion", null, () -> clienteLogistica.camion(patente))
            .con("patente", patente)
            .con("errorDelFormulario", errorDelFormulario)
            .mapa());
  }

  private void mostrarConfirmacion(Context ctx, Long entregaId, String errorDelFormulario) {
    Modelo modelo = Modelo.de("Confirmar recepción").seccion("Confirmar recepción")
        .comoBeneficiaria().activa("confirmar")
        .con("errorDelFormulario", errorDelFormulario);

    if (entregaId == null) {
      sinIdentidad(modelo);
    } else {
      modelo.con("entregaId", entregaId)
          .conDatosDe("entrega", null, () -> clienteLogistica.entrega(entregaId));
    }

    ctx.render("confirmar-recepcion.html.hbs", modelo.mapa());
  }

  private void sinIdentidad(Modelo modelo) {
    modelo.con("errorDeSesion", identidad.comoIdentificarse());
  }

  private FlotaVista flota() {
    return new FlotaVista(clienteLogistica.camiones());
  }

  private double numero(Context ctx, String campo) {
    return ctx.formParamAsClass(campo, Double.class).get();
  }
}
