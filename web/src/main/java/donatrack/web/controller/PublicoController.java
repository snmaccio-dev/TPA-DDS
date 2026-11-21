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

import donatrack.web.vista.EntregaPublicaVista;
import donatrack.web.vista.MapaDeEntregasVista;

import java.util.List;
import java.util.Objects;

public class PublicoController {

  private static final String DONANTE = "donante";
  private static final String BENEFICIARIA = "beneficiaria";
  static final int ENTREGAS_RECIENTES = 5;

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
    // Compatibilidad con links viejos: el ingreso es directo contra Keycloak.
    app.get("/login", ctx -> ctx.redirect("/sesion/ingresar"));
    app.get("/privacidad", this::privacidad);
  }

  // La landing muestra las ultimas entregas confirmadas: Logistica ya las recorta.
  private void landing(Context ctx) {
    ctx.render("index.html.hbs",
        Modelo.de("Inicio")
            .conDatosDe("entregas", List.<EntregaPublicaVista>of(),
                () -> clienteLogistica.entregasPublicasRecientes(ENTREGAS_RECIENTES))
            .mapa());
  }

  // El mapa necesita todas las entregas confirmadas; la lista y el carrusel, solo las
  // ultimas. Se piden todas una sola vez y las recientes salen de ahi.
  private void donacionesPublicas(Context ctx) {
    Modelo modelo = Modelo.de("Donaciones entregadas").conMapa();
    List<EntregaPublicaVista> entregas = modelo.cargar(
        "entregas", List.<EntregaPublicaVista>of(), clienteLogistica::entregasPublicas);
    List<EntregaPublicaVista> recientes =
        EntregaPublicaVista.recientes(entregas, ENTREGAS_RECIENTES);

    ctx.render("donaciones-publicas.html.hbs",
        modelo
            .con("recientes", recientes)
            .con("recientesConFoto",
                recientes.stream().filter(EntregaPublicaVista::getTieneFoto).toList())
            .con("mapaEntregas", mapaDe(entregas, modelo))
            .mapa());
  }

  // Ubicaciones de todas las entidades que recibieron entregas, en una sola consulta. Si
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
    mostrarRegistro(ctx, null);
  }

  private void crearCuenta(Context ctx) {
    String perfil = ctx.formParam("perfil");
    String email = ctx.formParam("email");

    if (email == null || email.isBlank()) {
      mostrarRegistro(ctx, "El correo es obligatorio.");
      return;
    }

    try {
      if (DONANTE.equals(perfil)) {
        clienteDonaciones.registrarDonante(donanteDelFormulario(ctx));
      } else if (BENEFICIARIA.equals(perfil)) {
        clienteDonaciones.registrarBeneficiaria(beneficiariaDelFormulario(ctx));
      } else {
        mostrarRegistro(ctx, "Elegí un tipo de cuenta.");
        return;
      }
    } catch (IntegracionException e) {
      mostrarRegistro(ctx, mensajeDe(e));
      return;
    }

    ctx.redirect("/sesion/ingresar");
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

  private void mostrarRegistro(Context ctx, String alertaError) {
    ctx.render("registro.html.hbs",
        Modelo.de("Crear cuenta").conAlertaError(alertaError).mapa());
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
