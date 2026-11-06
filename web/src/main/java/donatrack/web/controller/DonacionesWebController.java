package donatrack.web.controller;

import donatrack.web.Alertas;
import donatrack.web.Modelo;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.FiltroDonacionesWeb;
import donatrack.web.integracion.IntegracionException;
import donatrack.web.integracion.NuevaDonacion;
import donatrack.web.integracion.NuevaNecesidad;
import donatrack.web.integracion.NuevoDonanteAdmin;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.integracion.ResultadoEjecucion;
import donatrack.web.integracion.ServicioNoDisponibleException;
import donatrack.web.sesion.IdentidadDelUsuario;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.vista.DonacionVista;
import donatrack.web.vista.DonanteVista;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.NotificacionVista;
import donatrack.web.vista.PerfilVista;
import donatrack.web.vista.PropuestaVista;
import donatrack.web.vista.SubcategoriaVista;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DonacionesWebController {

  private final ClienteDonaciones clienteDonaciones;
  private final IdentidadDelUsuario identidad;

  public DonacionesWebController(ClienteDonaciones clienteDonaciones,
                                 IdentidadDelUsuario identidad) {
    this.clienteDonaciones = clienteDonaciones;
    this.identidad = identidad;
  }

  public void registrarRutas(Javalin app) {
    app.get("/donante-dashboard", this::panelDonante);
    app.get("/mis-donaciones", this::misDonaciones);
    app.get("/entidades", this::entidades);

    app.get("/beneficiario-dashboard", this::panelBeneficiaria);
    app.get("/necesidades", this::necesidades);
    app.post("/necesidades", this::crearNecesidad);
    app.get("/donaciones-asignadas", this::donacionesAsignadas);

    app.get("/admin-dashboard", this::panelAdmin);
    app.get("/admin-donantes", this::donantes);
    app.post("/admin-donantes", this::crearDonanteAdmin);
    app.get("/deposito", this::deposito);
    app.post("/deposito", this::registrarIngreso);
    app.post("/deposito/{id}/vencer", this::marcarVencida);
    app.get("/asignaciones", this::asignaciones);
    app.post("/asignaciones/{id}", this::confirmarAsignacion);
    app.post("/asignaciones/ejecutar", this::ejecutarAsignaciones);
    app.get("/importar-csv", this::importarCsv);

    app.get("/notificaciones", this::notificaciones);
    app.post("/notificaciones/marcar-leidas", this::marcarNotificacionesLeidas);
  }

  private void panelDonante(Context ctx) {
    ctx.render("donante-dashboard.html.hbs",
        Modelo.de("Panel de donante").seccion("Panel").comoDonante().activa("panel").mapa());
  }

  private void misDonaciones(Context ctx) {
    FiltroDonacionesWeb filtro = new FiltroDonacionesWeb(
        ctx.queryParam("estado"),
        ctx.queryParam("categoria"),
        ctx.queryParams("subcategoria")
    );

    ctx.render("mis-donaciones.html.hbs",
        Modelo.de("Mis donaciones").seccion("Mis donaciones")
            .conUsuario(usuario(ctx).perfil())
            .comoDonante().activa("misDonaciones")
            .con("filtro", Map.of(
                "estado", filtro.estado() == null ? "" : filtro.estado(),
                "categoria", filtro.categoria() == null ? "" : filtro.categoria(),
                "subcategorias", filtro.subcategorias()))
            .conDatosDe("donaciones", List.<DonacionVista>of(),
                () -> clienteDonaciones.donaciones(autorizacionDe(ctx), filtro))
            .conDatosDe("subcategorias", List.<SubcategoriaVista>of(),
                () -> clienteDonaciones.subcategorias(autorizacionDe(ctx)))
            .con("categorias", categoriasDe(ctx))
            .mapa());
  }

  private List<String> categoriasDe(Context ctx) {
    try {
      Set<String> unicas = new LinkedHashSet<>();
      for (SubcategoriaVista sub : clienteDonaciones.subcategorias(autorizacionDe(ctx))) {
        if (sub.getCategoria() != null && !sub.getCategoria().isBlank()) {
          unicas.add(sub.getCategoria());
        }
      }
      return new ArrayList<>(unicas);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private void entidades(Context ctx) {
    ctx.render("entidades.html.hbs",
        Modelo.de("Entidades").seccion("Entidades")
            .conUsuario(usuario(ctx).perfil())
            .comoDonante().activa("entidades")
            .conDatosDe("entidades", List.<EntidadVista>of(),
                () -> clienteDonaciones.entidades(autorizacionDe(ctx)))
            .mapa());
  }

  private void panelBeneficiaria(Context ctx) {
    ctx.render("beneficiario-dashboard.html.hbs",
        Modelo.de("Panel de la entidad").seccion("Panel")
            .comoBeneficiaria().activa("panel").mapa());
  }

  private void necesidades(Context ctx) {
    mostrarNecesidades(ctx, null);
  }

  private void crearNecesidad(Context ctx) {
    String descripcion = ctx.formParam("descripcion");
    String subcategoria = ctx.formParam("subcategoria");
    Integer cantidad = parseEntero(ctx.formParam("cantidad"));

    if (descripcion == null || descripcion.isBlank()
        || subcategoria == null || subcategoria.isBlank()
        || cantidad == null || cantidad <= 0) {
      mostrarNecesidades(ctx, "Completá descripción, subcategoría y una cantidad válida.");
      return;
    }

    NuevaNecesidad nueva = new NuevaNecesidad(
        null,
        descripcion,
        cantidad,
        subcategoria,
        ctx.formParam("tipo"),
        ctx.formParam("periodo")
    );

    try {
      clienteDonaciones.crearNecesidad(autorizacionDe(ctx), nueva);
    } catch (IntegracionException e) {
      mostrarNecesidades(ctx, mensajeDe(e));
      return;
    }

    ctx.redirect("/necesidades");
  }

  private void mostrarNecesidades(Context ctx, String alertaError) {
    ctx.render("necesidades.html.hbs",
        Modelo.de("Mis necesidades").seccion("Necesidades")
            .conUsuario(usuario(ctx).perfil())
            .comoBeneficiaria().activa("necesidades")
            .conAlertaError(alertaError)
            .conDatosDe("necesidades", List.<NecesidadVista>of(),
                () -> clienteDonaciones.necesidades(autorizacionDe(ctx)))
            .conDatosDe("subcategorias", List.<SubcategoriaVista>of(),
                () -> clienteDonaciones.subcategorias(autorizacionDe(ctx)))
            .mapa());
  }

  private void donacionesAsignadas(Context ctx) {
    ctx.render("donaciones-asignadas.html.hbs",
        Modelo.de("Donaciones asignadas").seccion("Donaciones asignadas")
            .conUsuario(usuario(ctx).perfil())
            .comoBeneficiaria().activa("asignadas")
            .conDatosDe("donaciones", List.<DonacionVista>of(),
                () -> clienteDonaciones.donaciones(autorizacionDe(ctx), FiltroDonacionesWeb.vacio()))
            .mapa());
  }

  private void panelAdmin(Context ctx) {
    ctx.render("admin-dashboard.html.hbs",
        Modelo.de("Panel de administración").seccion("Panel")
            .comoAdmin().activa("panel").mapa());
  }

  private void donantes(Context ctx) {
    mostrarDonantes(ctx, null);
  }

  private void crearDonanteAdmin(Context ctx) {
    String tipo = ctx.formParam("tipo");
    String email = ctx.formParam("email");
    if (tipo == null || tipo.isBlank() || email == null || email.isBlank()) {
      mostrarDonantes(ctx, "El correo y el tipo de persona son obligatorios.");
      return;
    }

    NuevoDonanteAdmin datos = new NuevoDonanteAdmin(
        email,
        tipo,
        ctx.formParam("direccion"),
        ctx.formParam("nombre"),
        ctx.formParam("apellido"),
        parseEntero(ctx.formParam("edad")),
        ctx.formParam("documento"),
        ctx.formParam("genero"),
        ctx.formParam("cuit"),
        ctx.formParam("razonSocial"),
        ctx.formParam("tipoOrganizacion"),
        ctx.formParam("rubro")
    );

    try {
      clienteDonaciones.registrarDonanteAdmin(autorizacionDe(ctx), datos);
    } catch (IntegracionException e) {
      mostrarDonantes(ctx, mensajeDe(e));
      return;
    }

    ctx.redirect("/admin-donantes");
  }

  private void mostrarDonantes(Context ctx, String alertaError) {
    ctx.render("admin-donantes.html.hbs",
        Modelo.de("Donantes").seccion("Donantes")
            .conUsuario(usuario(ctx).perfil())
            .comoAdmin().activa("donantes")
            .conAlertaError(alertaError)
            .conDatosDe("donantes", List.<DonanteVista>of(),
                () -> clienteDonaciones.donantes(autorizacionDe(ctx)))
            .mapa());
  }

  private void deposito(Context ctx) {
    mostrarDeposito(ctx, null);
  }

  private void mostrarDeposito(Context ctx, String alertaError) {
    FiltroDonacionesWeb filtro = new FiltroDonacionesWeb(
        ctx.queryParam("estado"),
        ctx.queryParam("categoria"),
        List.of()
    );

    ctx.render("deposito.html.hbs",
        Modelo.de("Depósito").seccion("Depósito")
            .conUsuario(usuario(ctx).perfil())
            .comoAdmin().activa("deposito")
            .conAlertaError(alertaError)
            .con("filtro", Map.of(
                "estado", filtro.estado() == null ? "" : filtro.estado(),
                "categoria", filtro.categoria() == null ? "" : filtro.categoria()))
            .conDatosDe("donaciones", List.<DonacionVista>of(),
                () -> clienteDonaciones.donaciones(autorizacionDe(ctx), filtro))
            .conDatosDe("donantes", List.<DonanteVista>of(),
                () -> clienteDonaciones.donantes(autorizacionDe(ctx)))
            .conDatosDe("subcategorias", List.<SubcategoriaVista>of(),
                () -> clienteDonaciones.subcategorias(autorizacionDe(ctx)))
            .con("categorias", categoriasDe(ctx))
            .mapa());
  }

  private void registrarIngreso(Context ctx) {
    String donanteDocumento = ctx.formParam("donanteDocumento");
    String descripcion = ctx.formParam("descripcion");
    String bienDescripcion = ctx.formParam("bienDescripcion");
    String subcategoria = ctx.formParam("subcategoria");
    Double cantidad = parseDecimal(ctx.formParam("cantidad"));

    if (donanteDocumento == null || donanteDocumento.isBlank()
        || descripcion == null || descripcion.isBlank()
        || bienDescripcion == null || bienDescripcion.isBlank()
        || subcategoria == null || subcategoria.isBlank()
        || cantidad == null || cantidad <= 0) {
      mostrarDeposito(ctx, "Completá todos los campos obligatorios del ingreso.");
      return;
    }

    NuevaDonacion.Bien bien = new NuevaDonacion.Bien(
        bienDescripcion,
        subcategoria,
        cantidad,
        ctx.formParam("condicion"),
        ctx.formParam("fechaVencimiento")
    );
    NuevaDonacion datos = new NuevaDonacion(donanteDocumento, descripcion, List.of(bien));

    try {
      clienteDonaciones.crearDonacion(autorizacionDe(ctx), datos);
    } catch (IntegracionException e) {
      mostrarDeposito(ctx, mensajeDe(e));
      return;
    }

    ctx.redirect("/deposito");
  }

  private void marcarVencida(Context ctx) {
    long id = ctx.pathParamAsClass("id", Long.class).get();
    try {
      clienteDonaciones.marcarDonacionVencida(autorizacionDe(ctx), id);
    } catch (IntegracionException e) {
      Alertas.flashError(ctx, mensajeDe(e));
    }
    ctx.redirect("/deposito");
  }

  private void asignaciones(Context ctx) {
    mostrarAsignaciones(ctx, null);
  }

  private void mostrarAsignaciones(Context ctx, String alertaError) {
    List<PropuestaVista> propuestas = new ArrayList<>();
    Modelo modelo = Modelo.de("Asignaciones").seccion("Asignaciones")
        .conUsuario(usuario(ctx).perfil())
        .comoAdmin().activa("asignaciones")
        .conAlertaError(alertaError)
        .conDatosDe("propuestas", List.<PropuestaVista>of(),
            () -> propuestasEnriquecidas(ctx));

    // Advertencia por propuestas sin candidatas: concatenamos los ids en un solo
    // mensaje para no romper el patron de "una alerta a la vez".
    Object cargadas = modelo.mapa().get("propuestas");
    if (cargadas instanceof List<?> lista) {
      for (Object item : lista) {
        if (item instanceof PropuestaVista p && !p.isHuboCoincidencias()) {
          propuestas.add(p);
        }
      }
    }
    if (!propuestas.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < propuestas.size(); i++) {
        if (i > 0) sb.append(" ");
        sb.append("La donación #").append(propuestas.get(i).getDonacionId())
            .append(" no tiene entidades compatibles.");
      }
      modelo.conAlertaAdvertencia(sb.toString());
    }

    ctx.render("asignaciones.html.hbs", modelo.mapa());
  }

  private List<PropuestaVista> propuestasEnriquecidas(Context ctx) {
    List<PropuestaVista> propuestas =
        clienteDonaciones.propuestasDeAsignacion(autorizacionDe(ctx));
    if (propuestas.isEmpty()) {
      return propuestas;
    }

    Map<Long, String> descripciones;
    try {
      List<DonacionVista> todas =
          clienteDonaciones.donaciones(autorizacionDe(ctx), FiltroDonacionesWeb.vacio());
      Map<Long, String> acum = new HashMap<>();
      for (DonacionVista d : todas) {
        acum.put(d.getId(), d.getDescripcion());
      }
      descripciones = acum;
    } catch (RuntimeException e) {
      descripciones = Map.of();
    }

    List<PropuestaVista> enriquecidas = new ArrayList<>(propuestas.size());
    for (PropuestaVista p : propuestas) {
      enriquecidas.add(p.conDescripcion(descripciones.get(p.getDonacionId())));
    }
    return enriquecidas;
  }

  private void confirmarAsignacion(Context ctx) {
    Long beneficiariaId = parseLargo(ctx.formParam("beneficiariaId"));
    long id = ctx.pathParamAsClass("id", Long.class).get();
    if (beneficiariaId == null) {
      mostrarAsignaciones(ctx, "Seleccioná una entidad beneficiaria.");
      return;
    }

    try {
      clienteDonaciones.confirmarDestinatario(autorizacionDe(ctx), id, beneficiariaId);
    } catch (IntegracionException e) {
      Alertas.flashError(ctx, mensajeDe(e));
    }
    ctx.redirect("/asignaciones");
  }

  private void ejecutarAsignaciones(Context ctx) {
    try {
      ResultadoEjecucion resultado = clienteDonaciones.ejecutarAsignaciones(autorizacionDe(ctx));
      if (resultado.cantidadFallidas() > 0) {
        Alertas.flashAdvertencia(ctx,
            "Se asignaron " + resultado.procesadas() + " donaciones; "
                + resultado.cantidadFallidas() + " no pudieron procesarse.");
      }
    } catch (IntegracionException e) {
      Alertas.flashError(ctx, mensajeDe(e));
    }
    ctx.redirect("/asignaciones");
  }

  private void importarCsv(Context ctx) {
    ctx.render("importar-csv.html.hbs",
        Modelo.de("Importar CSV").seccion("Importar CSV")
            .conUsuario(usuario(ctx).perfil())
            .comoAdmin().activa("importar")
            .conAlertaAdvertencia("Funcionalidad no disponible todavía")
            .mapa());
  }

  private void notificaciones(Context ctx) {
    UsuarioWeb u = usuario(ctx);
    Modelo modelo = Modelo.de("Notificaciones").seccion("Notificaciones")
        .conUsuario(u.perfil())
        .conDatosDe("notificaciones", List.<NotificacionVista>of(),
            () -> clienteDonaciones.misNotificaciones(u.autorizacion()));
    aplicarRol(modelo, u).activa("notificaciones");
    ctx.render("notificaciones.html.hbs", modelo.mapa());
  }

  private void marcarNotificacionesLeidas(Context ctx) {
    try {
      clienteDonaciones.marcarTodasLasNotificacionesLeidas(autorizacionDe(ctx));
    } catch (IntegracionException e) {
      Alertas.flashError(ctx, mensajeDe(e));
    }
    ctx.redirect("/notificaciones");
  }

  private Modelo aplicarRol(Modelo modelo, UsuarioWeb usuario) {
    PerfilVista perfil = usuario.perfil();
    if (perfil == null) {
      return modelo.comoDonante();
    }
    if (perfil.isAdministrador()) return modelo.comoAdmin();
    if (perfil.isBeneficiaria()) return modelo.comoBeneficiaria();
    if (perfil.isChofer()) return modelo.comoChofer();
    return modelo.comoDonante();
  }

  // Traduce las excepciones de integracion al texto que va en la alerta.
  static String mensajeDe(IntegracionException e) {
    if (e instanceof ServicioNoDisponibleException) {
      return Alertas.SERVICIO_NO_DISPONIBLE;
    }
    if (e instanceof OperacionRechazadaException) {
      return e.getMessage();
    }
    return e.getMessage();
  }

  private static Integer parseEntero(String texto) {
    if (texto == null || texto.isBlank()) return null;
    try {
      return Integer.parseInt(texto.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Long parseLargo(String texto) {
    if (texto == null || texto.isBlank()) return null;
    try {
      return Long.parseLong(texto.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Double parseDecimal(String texto) {
    if (texto == null || texto.isBlank()) return null;
    try {
      return Double.parseDouble(texto.trim());
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
}
