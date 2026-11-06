package donatrack.web.controller;

import donatrack.web.Modelo;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.integracion.RegistroBeneficiaria;
import donatrack.web.integracion.RegistroDonante;
import donatrack.web.integracion.ServicioNoDisponibleException;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Objects;

public class PublicoController {

  private static final String DONANTE = "donante";
  private static final String BENEFICIARIA = "beneficiaria";

  private final ClienteDonaciones clienteDonaciones;

  public PublicoController(ClienteDonaciones clienteDonaciones) {
    Objects.requireNonNull(clienteDonaciones, "Debe indicarse el cliente de Donaciones.");
    this.clienteDonaciones = clienteDonaciones;
  }

  public void registrarRutas(Javalin app) {
    app.get("/", this::landing);
    app.get("/donaciones-publicas", this::donacionesPublicas);
    app.get("/registro", this::registro);
    app.post("/registro", this::crearCuenta);
    app.get("/login", this::login);
    app.get("/privacidad", this::privacidad);
  }

  private void landing(Context ctx) {
    ctx.render("index.html.hbs", Modelo.de("Inicio").mapa());
  }

  private void donacionesPublicas(Context ctx) {
    ctx.render("donaciones-publicas.html.hbs",
        Modelo.de("Donaciones entregadas").conMapa().mapa());
  }

  private void registro(Context ctx) {
    mostrarRegistro(ctx, null);
  }

  private void crearCuenta(Context ctx) {
    String perfil = ctx.formParam("perfil");

    try {
      if (DONANTE.equals(perfil)) {
        clienteDonaciones.registrarDonante(donanteDelFormulario(ctx));
      } else if (BENEFICIARIA.equals(perfil)) {
        clienteDonaciones.registrarBeneficiaria(beneficiariaDelFormulario(ctx));
      } else {
        mostrarRegistro(ctx, "Elegi un tipo de cuenta.");
        return;
      }
    } catch (OperacionRechazadaException e) {
      // El mensaje viene de Donaciones y ya esta en lenguaje de dominio: documento
      // repetido, cuit invalido, email en uso. Se muestra tal cual.
      mostrarRegistro(ctx, e.getMessage());
      return;
    } catch (ServicioNoDisponibleException e) {
      mostrarRegistro(ctx, "El servicio no esta disponible en este momento. Proba de nuevo.");
      return;
    }

    ctx.redirect("/login?cuentaCreada=1");
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

  private void mostrarRegistro(Context ctx, String error) {
    ctx.render("registro.html.hbs",
        Modelo.de("Crear cuenta").con("error", error).mapa());
  }

  private void login(Context ctx) {
    ctx.render("login.html.hbs",
        Modelo.de("Iniciar sesión")
            .con("cuentaCreada", ctx.queryParam("cuentaCreada") != null)
            .mapa());
  }

  private void privacidad(Context ctx) {
    ctx.render("privacidad.html.hbs", Modelo.de("Privacidad").mapa());
  }

  private static Integer entero(String valor) {
    try {
      return valor == null || valor.isBlank() ? null : Integer.valueOf(valor.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
