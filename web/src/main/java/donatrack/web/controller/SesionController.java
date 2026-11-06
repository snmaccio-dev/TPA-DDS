package donatrack.web.controller;

import donatrack.web.Modelo;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.sesion.ClienteOidc;
import donatrack.web.sesion.IdentidadPorSesion;
import donatrack.web.sesion.Pkce;
import donatrack.web.sesion.TokensDeKeycloak;
import donatrack.web.sesion.UsuarioWeb;
import donatrack.web.vista.PerfilVista;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.security.SecureRandom;
import java.util.Base64;

public class SesionController {

  private static final String CLAVE_ESTADO = "estadoDeIngreso";
  private static final String CLAVE_VERIFICADOR = "verificadorDeIngreso";

  private static final SecureRandom AZAR = new SecureRandom();
  private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

  private final ClienteOidc oidc;
  private final ClienteDonaciones clienteDonaciones;

  public SesionController(ClienteOidc oidc, ClienteDonaciones clienteDonaciones) {
    this.oidc = oidc;
    this.clienteDonaciones = clienteDonaciones;
  }

  public void registrarRutas(Javalin app) {
    app.get("/sesion/ingresar", this::ingresar);
    app.get("/callback", this::recibirCodigo);
    app.post("/logout", this::salir);
  }

  private void ingresar(Context ctx) {
    String estado = nuevoValorAlAzar();
    String verificador = Pkce.nuevoVerificador();

    ctx.sessionAttribute(CLAVE_ESTADO, estado);
    ctx.sessionAttribute(CLAVE_VERIFICADOR, verificador);

    ctx.redirect(oidc.urlDeIngreso(estado, Pkce.desafioDe(verificador)));
  }

  private void recibirCodigo(Context ctx) {
    String estadoEsperado = ctx.sessionAttribute(CLAVE_ESTADO);
    String verificador = ctx.sessionAttribute(CLAVE_VERIFICADOR);

    ctx.sessionAttribute(CLAVE_ESTADO, null);
    ctx.sessionAttribute(CLAVE_VERIFICADOR, null);

    String codigo = ctx.queryParam("code");
    String estadoRecibido = ctx.queryParam("state");

    if (codigo == null || estadoEsperado == null || !estadoEsperado.equals(estadoRecibido)) {
      mostrarFalloDeIngreso(ctx, "El ingreso no se pudo completar. Volvé a intentarlo.");
      return;
    }

    TokensDeKeycloak tokens = oidc.intercambiar(codigo, verificador);
    String autorizacion = "Bearer " + tokens.accessToken();
    PerfilVista perfil = clienteDonaciones.perfil(autorizacion);

    ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO, UsuarioWeb.de(tokens, perfil));

    ctx.redirect(perfil.inicioSegunRol());
  }

  private void salir(Context ctx) {
    UsuarioWeb usuario = ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO);

    ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO, null);

    ctx.redirect(oidc.urlDeSalida(usuario == null ? null : usuario.idToken()));
  }

  private void mostrarFalloDeIngreso(Context ctx, String mensaje) {
    ctx.status(400).render("login.html.hbs",
        Modelo.de("Ingresar").seccion("Ingresar").con("errorDeIngreso", mensaje).mapa());
  }

  private String nuevoValorAlAzar() {
    byte[] bytes = new byte[24];
    AZAR.nextBytes(bytes);

    return BASE64.encodeToString(bytes);
  }
}
