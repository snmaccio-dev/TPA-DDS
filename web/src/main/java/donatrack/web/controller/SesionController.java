package donatrack.web.controller;

import donatrack.web.Alertas;
import donatrack.web.integracion.ClienteDonaciones;
import donatrack.web.integracion.IntegracionException;
import donatrack.web.integracion.OperacionRechazadaException;
import donatrack.web.integracion.ServicioNoDisponibleException;
import donatrack.web.sesion.ClienteOidc;
import donatrack.web.sesion.IdentidadPorSesion;
import donatrack.web.sesion.Pkce;
import donatrack.web.sesion.SesionExpiradaException;
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
    // Correo de la cuenta recien creada en /registro. Viaja en la sesion de la web (no en
    // la URL): lo muestra /registro/exito y lo consume el ingreso que sigue al registro.
    public static final String CLAVE_EMAIL_REGISTRADO = "emailRegistrado";

    private static final String LANDING = "/";

    private static final String INGRESO_INCOMPLETO =
        "El ingreso no se pudo completar. Volvé a intentarlo.";
    private static final String SIN_PERMISOS =
        "Tu cuenta no tiene permisos para ingresar a DonaTrack. "
            + "Cerramos la sesión para que puedas entrar con otra cuenta.";

    private static final SecureRandom AZAR = new SecureRandom();
    private static final Base64.Encoder BASE64 =
        Base64.getUrlEncoder().withoutPadding();

    private final ClienteOidc oidc;
    private final ClienteDonaciones clienteDonaciones;

    public SesionController(
        ClienteOidc oidc,
        ClienteDonaciones clienteDonaciones
    ) {
        this.oidc = oidc;
        this.clienteDonaciones = clienteDonaciones;
    }

    public void registrarRutas(Javalin app) {
        // Unico ingreso: el destino se decide por rol despues del callback.
        app.get("/sesion/ingresar", this::ingresar);
        app.get("/callback", this::recibirCodigo);
        app.post("/logout", this::salir);
    }

    // Despues de un registro (?registro=1) se pide prompt=login: si Keycloak tiene abierta
    // la sesion SSO de otra cuenta, igual muestra el formulario en lugar de entrar con esa
    // cuenta. login_hint completa el correo recien registrado. El ingreso normal no cambia.
    private void ingresar(Context ctx) {
        String estado = nuevoValorAlAzar();
        String verificador = Pkce.nuevoVerificador();

        boolean vieneDeRegistro = "1".equals(ctx.queryParam("registro"));
        String emailRegistrado = ctx.sessionAttribute(CLAVE_EMAIL_REGISTRADO);
        ctx.sessionAttribute(CLAVE_EMAIL_REGISTRADO, null);

        ctx.sessionAttribute(CLAVE_ESTADO, estado);
        ctx.sessionAttribute(CLAVE_VERIFICADOR, verificador);

        ctx.redirect(
            oidc.urlDeIngreso(
                estado,
                Pkce.desafioDe(verificador),
                vieneDeRegistro,
                vieneDeRegistro ? emailRegistrado : null
            )
        );
    }

    private void recibirCodigo(Context ctx) {
        String estadoEsperado =
            ctx.sessionAttribute(CLAVE_ESTADO);

        String verificador =
            ctx.sessionAttribute(CLAVE_VERIFICADOR);

        ctx.sessionAttribute(CLAVE_ESTADO, null);
        ctx.sessionAttribute(CLAVE_VERIFICADOR, null);

        String codigo = ctx.queryParam("code");
        String estadoRecibido = ctx.queryParam("state");

        // State invalido o sesion perdida: fallo pasajero, volvemos a la landing sin
        // reenviar a Keycloak para no entrar en un bucle.
        if (codigo == null
            || estadoEsperado == null
            || verificador == null
            || !estadoEsperado.equals(estadoRecibido)) {

            falloPasajero(ctx, INGRESO_INCOMPLETO);
            return;
        }

        TokensDeKeycloak tokens;

        try {
            tokens = oidc.intercambiar(codigo, verificador);
        } catch (RuntimeException e) {
            falloPasajero(ctx, INGRESO_INCOMPLETO);
            return;
        }

        PerfilVista perfil;

        try {
            perfil = clienteDonaciones.perfil(
                "Bearer " + tokens.accessToken()
            );
        } catch (SesionExpiradaException e) {
            // /usuarios/yo respondio 401: Donaciones no acepta esta cuenta.
            falloDePermisos(ctx, tokens, SIN_PERMISOS);
            return;
        } catch (OperacionRechazadaException e) {
            // 403/404 (u otro 4xx): la cuenta autentico pero no tiene perfil en la app.
            falloDePermisos(ctx, tokens, SIN_PERMISOS);
            return;
        } catch (ServicioNoDisponibleException e) {
            falloPasajero(ctx,
                "Te autenticaste bien, pero no se pudo completar el ingreso: "
                    + Alertas.SERVICIO_NO_DISPONIBLE);
            return;
        } catch (IntegracionException e) {
            falloPasajero(ctx,
                "Te autenticaste bien, pero no se pudo completar el ingreso: "
                    + e.getMessage());
            return;
        }

        if (!tieneRolDeLaApp(perfil)) {
            falloDePermisos(ctx, tokens, SIN_PERMISOS);
            return;
        }

        ctx.sessionAttribute(
            IdentidadPorSesion.CLAVE_USUARIO,
            UsuarioWeb.de(tokens, perfil)
        );

        ctx.redirect(perfil.inicioSegunRol());
    }

    private void salir(Context ctx) {
        UsuarioWeb usuario =
            ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO);

        ctx.sessionAttribute(
            IdentidadPorSesion.CLAVE_USUARIO,
            null
        );

        ctx.redirect(
            oidc.urlDeSalida(
                usuario == null ? null : usuario.idToken()
            )
        );
    }

    private static boolean tieneRolDeLaApp(PerfilVista perfil) {
        return perfil != null
            && (perfil.isAdministrador()
                || perfil.isBeneficiaria()
                || perfil.isDonante()
                || perfil.isChofer());
    }

    // Fallo pasajero: mostramos el motivo en la landing y el usuario reintenta a mano.
    private void falloPasajero(Context ctx, String mensaje) {
        ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO, null);
        Alertas.flashError(ctx, mensaje);
        ctx.redirect(LANDING);
    }

    // Fallo de permisos o de perfil: cerramos la sesion de Keycloak (si no, el SSO nos
    // devolveria la misma cuenta) y Keycloak vuelve a la landing, donde se ve el flash
    // guardado en la sesion de la web.
    private void falloDePermisos(Context ctx, TokensDeKeycloak tokens, String mensaje) {
        ctx.sessionAttribute(IdentidadPorSesion.CLAVE_USUARIO, null);
        Alertas.flashError(ctx, mensaje);
        ctx.redirect(oidc.urlDeSalida(tokens.idToken()));
    }

    private String nuevoValorAlAzar() {
        byte[] bytes = new byte[24];
        AZAR.nextBytes(bytes);

        return BASE64.encodeToString(bytes);
    }
}
