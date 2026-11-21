package donatrack.web.controller;

import donatrack.web.Alertas;
import donatrack.web.Modelo;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.ClienteLogistica;
import donatrack.web.integracion.IntegracionException;
import donatrack.web.integracion.RegistroBeneficiaria;
import donatrack.web.integracion.RegistroDonante;
import donatrack.web.integracion.ServicioNoDisponibleException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpSession;

import donatrack.web.sesion.IdentidadPorSesion;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.vista.EntregaPublicaVista;
import donatrack.web.vista.MapaDeEntregasVista;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PublicoController {

  private static final String DONANTE = "donante";
  private static final String BENEFICIARIA = "beneficiaria";
  // /donaciones-publicas: mapa, lista y carrusel.
  static final int ENTREGAS_RECIENTES = 5;
  private static final String CAMPO_CONTRASENA = "contrasena";
  private static final String REGISTRO_EXITOSO = "/registro/exito";
  // Landing: las ultimas entregas, todas visibles sin paginar.
  static final int ENTREGAS_LANDING = 3;

  private final ClienteDonaciones clienteDonaciones;
  private final ClienteLogistica clienteLogistica;

  public PublicoController(ClienteDonaciones clienteDonaciones,
                           ClienteLogistica clienteLogistica) {
    Objects.requireNonNull(clienteDonaciones, "Debe indicarse el cliente de Donaciones.");
    Objects.requireNonNull(clienteLogistica, "Debe indicarse el cliente de Logistica.");
    this.clienteDonaciones = clienteDonaciones;
    this.clienteLogistica = clienteLogistica;
  }

  public void registrarRutas(Javalin app) {
    app.get("/", this::landing);
    app.get("/donaciones-publicas", this::donacionesPublicas);
    app.get("/registro", this::registro);
    app.post("/registro", this::crearCuenta);
    app.get("/registro/exito", this::registroExitoso);
    // Compatibilidad con links viejos: el ingreso es directo contra Keycloak.
    app.get("/login", ctx -> ctx.redirect("/sesion/ingresar"));
    app.get("/privacidad", this::privacidad);
  }

  // La landing muestra las ultimas 3 entregas confirmadas: Logistica ya las recorta.
  private void landing(Context ctx) {
    ctx.render("index.html.hbs",
        Modelo.de("Inicio")
            .conDatosDe("entregas", List.<EntregaPublicaVista>of(),
                () -> clienteLogistica.entregasPublicasRecientes(ENTREGAS_LANDING))
            .mapa());
  }

  // Mapa, lista y carrusel usan las mismas entregas: las ultimas 5 confirmadas, por fecha
  // de entrega descendente. Logistica ya las recorta; se reordena igual para que las tres
  // vistas no dependan de ese detalle.
  private void donacionesPublicas(Context ctx) {
    Modelo modelo = Modelo.de("Donaciones entregadas").conMapa();
    List<EntregaPublicaVista> recientes = EntregaPublicaVista.recientes(
        modelo.cargar("recientes", List.<EntregaPublicaVista>of(),
            () -> clienteLogistica.entregasPublicasRecientes(ENTREGAS_RECIENTES)),
        ENTREGAS_RECIENTES);

    ctx.render("donaciones-publicas.html.hbs",
        modelo
            .con("recientes", recientes)
            .con("recientesConFoto",
                recientes.stream().filter(EntregaPublicaVista::getTieneFoto).toList())
            .con("mapaEntregas", mapaDe(recientes, modelo))
            .mapa());
  }

  // Ubicaciones de las entidades de esas entregas, en una sola consulta. Si
  // Donaciones no responde, el mapa queda sin marcadores y la pagina sigue igual.
  private MapaDeEntregasVista mapaDe(List<EntregaPublicaVista> entregas, Modelo modelo) {
    if (entregas.isEmpty()) {
      return MapaDeEntregasVista.vacio(entregas);
    }
    List<Long> entidades = entregas.stream()
        .map(EntregaPublicaVista::beneficiariaId)
        .distinct()
        .toList();
    try {
      return MapaDeEntregasVista.de(entregas, clienteDonaciones.ubicacionesDeEntidades(entidades));
    } catch (IntegracionException e) {
      modelo.con("ubicacionesNoDisponible", true);
      return MapaDeEntregasVista.vacio(entregas);
    }
  }

  private void registro(Context ctx) {
    if (redirigirSiYaInicioSesion(ctx)) {
      return;
    }
    mostrarRegistro(ctx, null, Map.of());
  }

  private void crearCuenta(Context ctx) {
    if (redirigirSiYaInicioSesion(ctx)) {
      return;
    }
    String perfil = ctx.formParam("perfil");
    String email = ctx.formParam("email");

    if (email == null || email.isBlank()) {
      mostrarRegistro(ctx, "El correo es obligatorio.", datosCargados(ctx));
      return;
    }

    try {
      if (DONANTE.equals(perfil)) {
        clienteDonaciones.registrarDonante(donanteDelFormulario(ctx));
      } else if (BENEFICIARIA.equals(perfil)) {
        clienteDonaciones.registrarBeneficiaria(beneficiariaDelFormulario(ctx));
      } else {
        mostrarRegistro(ctx, "Elegí un tipo de cuenta.", datosCargados(ctx));
        return;
      }
    } catch (IntegracionException e) {
      // El mensaje de Donaciones dice el campo y la regla; se muestra en el formulario con
      // los datos que se habian cargado.
      mostrarRegistro(ctx, mensajeDe(e), datosCargados(ctx));
      return;
    }

    cuentaCreada(ctx, email.trim());
  }

  // Registro exitoso: no queda ningun usuario de la web en sesion y se muestra la
  // confirmacion. El correo va en la sesion, no en la URL: lo usan la confirmacion y el
  // ingreso que sigue, que pide el formulario de Keycloak (aunque haya una sesion SSO de
  // otra cuenta) con ese correo.
  private static void cuentaCreada(Context ctx, String email) {
    HttpSession anterior = ctx.req().getSession(false);
    if (anterior != null) {
      anterior.invalidate();
    }
    ctx.sessionAttribute(SesionController.CLAVE_EMAIL_REGISTRADO, email);
    ctx.redirect(REGISTRO_EXITOSO);
  }

  // Solo tiene sentido justo despues de un registro: sin correo registrado en la sesion
  // (entrada directa, o ya se inicio sesion con esa cuenta) se va a la landing.
  private void registroExitoso(Context ctx) {
    String email = ctx.sessionAttribute(SesionController.CLAVE_EMAIL_REGISTRADO);
    if (email == null || email.isBlank()) {
      ctx.redirect("/");
      return;
    }
    ctx.render("registro-exito.html.hbs",
        Modelo.de("Registro exitoso").con("emailRegistrado", email).mapa());
  }

  // Con una sesion de la web ya iniciada, registrarse no tiene sentido: se va al panel.
  private static boolean redirigirSiYaInicioSesion(Context ctx) {
    UsuarioWeb usuario = ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO);
    if (usuario == null) {
      return false;
    }
    ctx.redirect(usuario.perfil() == null ? "/" : usuario.perfil().inicioSegunRol());
    return true;
  }

  private RegistroDonante donanteDelFormulario(Context ctx) {
    return RegistroDonante.humana(
        ctx.formParam("email"),
        ctx.formParam("contrasena"),
        ctx.formParam("direccion"),
        ctx.formParam("nombre"),
        ctx.formParam("apellido"),
        entero(ctx.formParam("edad")),
        ctx.formParam("documento"),
        ctx.formParam("genero")
    );
  }

  private RegistroBeneficiaria beneficiariaDelFormulario(Context ctx) {
    return new RegistroBeneficiaria(
        ctx.formParam("email"),
        ctx.formParam("contrasena"),
        ctx.formParam("cuit"),
        ctx.formParam("razonSocial"),
        ctx.formParam("tipoOrganizacion"),
        ctx.formParam("rubro"),
        ctx.formParam("direccion"),
        ctx.formParam("telefono"),
        List.of(new RegistroBeneficiaria.Representante(
            ctx.formParam("repNombre"),
            ctx.formParam("repApellido"),
            ctx.formParam("repEmail")
        ))
    );
  }

  private void mostrarRegistro(Context ctx, String error, Map<String, String> cargados) {
    Modelo modelo = Modelo.de("Crear cuenta").con("form", cargados);
    if (error != null && !error.isBlank()) {
      modelo.con("errorRegistro", error);
    }
    ctx.render("registro.html.hbs", modelo.mapa());
  }

  // Lo que se habia cargado en el formulario, para volver a mostrarlo si el registro falla.
  // La contrasena nunca vuelve a la pagina.
  private static Map<String, String> datosCargados(Context ctx) {
    Map<String, String> cargados = new HashMap<>();
    ctx.formParamMap().forEach((campo, valores) -> {
      if (!CAMPO_CONTRASENA.equals(campo) && !valores.isEmpty()) {
        cargados.put(campo, valores.get(0));
      }
    });
    return cargados;
  }

  private void privacidad(Context ctx) {
    ctx.render("privacidad.html.hbs", Modelo.de("Privacidad").mapa());
  }

  static String mensajeDe(IntegracionException e) {
    if (e instanceof ServicioNoDisponibleException) {
      return Alertas.SERVICIO_NO_DISPONIBLE;
    }
    return e.getMessage();
  }

  private static Integer entero(String valor) {
    try {
      return valor == null || valor.isBlank() ? null : Integer.valueOf(valor.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
