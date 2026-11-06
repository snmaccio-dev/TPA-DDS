package donatrack.donaciones;

import donatrack.donaciones.controller.AdministradoresController;
import donatrack.donaciones.controller.AsignacionesController;
import donatrack.donaciones.controller.DonacionesController;
import donatrack.donaciones.controller.DonantesController;
import donatrack.donaciones.controller.EntidadesBeneficiariasController;
import donatrack.donaciones.controller.NecesidadesController;
import donatrack.donaciones.controller.NotificacionesController;
import donatrack.donaciones.controller.RegistroController;
import donatrack.donaciones.controller.UsuariosController;
import donatrack.donaciones.controller.dto.ErrorDTO;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.TransicionNoPermitidaException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.infrastructure.identidad.ProveedorDeIdentidadKeycloak;
import donatrack.donaciones.infrastructure.notificacion.NotificadorEmail;
import donatrack.donaciones.infrastructure.notificacion.NotificadorSMS;
import donatrack.donaciones.infrastructure.notificacion.NotificadorWhatsApp;
import donatrack.donaciones.jobs.PlanificadorAsignaciones;
import donatrack.donaciones.jobs.PlanificadorAvisosInactividad;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.IntegracionConIdentidadException;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.ConsultaDeNotificaciones;
import donatrack.donaciones.service.EventoYaAplicadoException;
import donatrack.donaciones.service.GestorAsignaciones;
import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.service.GestorDonantes;
import donatrack.donaciones.service.GestorEntidadesBeneficiarias;
import donatrack.donaciones.service.GestorNecesidades;
import donatrack.donaciones.service.GestorNotificaciones;
import donatrack.donaciones.service.GestorPersonas;
import donatrack.donaciones.service.GestorRegistro;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;

import java.time.LocalTime;
import java.util.List;

public class DonacionesApp {

  private static final int PUERTO = 8080;
  private static final LocalTime HORARIO_MATCHMAKING = LocalTime.of(3, 0);
  private static final LocalTime HORARIO_AVISOS_INACTIVIDAD = LocalTime.of(9, 0);

  public static void main(String[] args) {
    String urlBase = variable("DONACIONES_URL_BASE", "http://localhost:" + PUERTO);

    // Configuracion de Keycloak. El client secret no tiene default: si falta
    // detenemos el arranque con un mensaje claro para que no se arranque un
    // backend sin forma de hablar con el proveedor de identidad.
    String keycloakUrl = variable("KEYCLOAK_URL", "http://localhost:8180");
    String keycloakRealm = variable("KEYCLOAK_REALM", "donatrack");
    String keycloakClientId = variable("KEYCLOAK_CLIENT_ID", "donaciones-backend");
    String keycloakClientSecret = variable("KEYCLOAK_CLIENT_SECRET", null);
    if (keycloakClientSecret == null || keycloakClientSecret.isBlank()) {
      throw new IllegalStateException(
          "Falta la variable KEYCLOAK_CLIENT_SECRET. Sin ella el microservicio "
              + "no puede autenticarse contra Keycloak."
      );
    }
    // El issuer en Docker puede diferir de la URL interna (por ejemplo,
    // http://keycloak:8080 interno vs http://localhost:8180 desde el navegador).
    // KEYCLOAK_ISSUER permite forzar el valor exacto que trae el JWT.
    String keycloakIssuer = variable("KEYCLOAK_ISSUER", null);

    // URL base del modulo web, usada para armar los links de la notificacion
    // INICIO_RUTA. En Docker suele diferir del localhost del desarrollador.
    String urlWeb = variable("WEB_URL_BASE", "http://localhost:8082");

    RepositorioPropuestas repositorioPropuestas = new RepositorioPropuestas();
    RepositorioSubcategorias repositorioSubcategorias = new RepositorioSubcategorias();
    sembrarSubcategorias(repositorioSubcategorias);

    RepositorioPersonas repositorioPersonas = RepositorioPersonas.getInstance();
    RepositorioEntidades repositorioEntidades = RepositorioEntidades.getInstance();
    RepositorioDonantes repositorioDonantes = new RepositorioDonantes();
    RepositorioUsuarios repositorioUsuarios = new RepositorioUsuarios();
    RepositorioNotificaciones repositorioNotificaciones = new RepositorioNotificaciones();

    ProveedorDeIdentidad proveedor = new ProveedorDeIdentidadKeycloak(
        keycloakUrl, keycloakRealm, keycloakClientId, keycloakClientSecret);

    VerificadorDeTokens verificador =
        new VerificadorDeTokens(keycloakUrl, keycloakRealm, keycloakIssuer);
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repositorioUsuarios);

    GestorNotificaciones gestorNotificaciones = new GestorNotificaciones(List.of(
        new NotificadorEmail(),
        new NotificadorSMS(),
        new NotificadorWhatsApp()
    ), repositorioNotificaciones);
    ConsultaDeNotificaciones consultaNotificaciones =
        new ConsultaDeNotificaciones(repositorioNotificaciones);
    GestorDonaciones gestorDonaciones = new GestorDonaciones(
        gestorNotificaciones,
        repositorioPropuestas,
        repositorioNotificaciones,
        urlWeb + "/seguimiento-donante",
        urlWeb + "/seguimiento-beneficiario"
    );
    GestorAsignaciones gestorAsignaciones =
        new GestorAsignaciones(repositorioPropuestas, gestorDonaciones);
    GestorEntidadesBeneficiarias gestorEntidades =
        new GestorEntidadesBeneficiarias(
            repositorioUsuarios,
            repositorioNotificaciones,
            donatrack.donaciones.repository.RepositorioNecesidades.getInstance(),
            repositorioPropuestas,
            RepositorioDonaciones.getInstance(),
            repositorioDonantes,
            proveedor);
    GestorNecesidades gestorNecesidades = new GestorNecesidades();
    GestorPersonas gestorPersonas =
        new GestorPersonas(
            repositorioDonantes,
            repositorioUsuarios,
            repositorioNotificaciones,
            RepositorioDonaciones.getInstance(),
            repositorioEntidades,
            proveedor);
    GestorDonantes gestorDonantes = new GestorDonantes(repositorioDonantes, gestorNotificaciones);
    GestorRegistro gestorRegistro = new GestorRegistro(
        proveedor, repositorioPersonas, repositorioDonantes, repositorioEntidades, repositorioUsuarios);

    Javalin app = Javalin.create().start(PUERTO);

    // El filtro se registra antes que las rutas: todas las rutas tienen
    // que declarar sus roles y el filtro las rechaza si no lo hacen.
    filtro.registrarEn(app);

    new RegistroController(gestorRegistro).registrarRutas(app);
    new UsuariosController().registrarRutas(app);
    new AdministradoresController(proveedor).registrarRutas(app);
    new DonacionesController(gestorDonaciones, repositorioPersonas, repositorioDonantes, repositorioSubcategorias).registrarRutas(app);
    new AsignacionesController(gestorAsignaciones, RepositorioDonaciones.getInstance(),
        repositorioEntidades).registrarRutas(app);
    new EntidadesBeneficiariasController(gestorEntidades).registrarRutas(app);
    new NecesidadesController(gestorNecesidades, repositorioSubcategorias, repositorioEntidades).registrarRutas(app);
    new DonantesController(gestorPersonas, gestorDonantes).registrarRutas(app);
    new NotificacionesController(consultaNotificaciones).registrarRutas(app);

    registrarManejoDeErrores(app);

    PlanificadorAsignaciones planificador = new PlanificadorAsignaciones(gestorAsignaciones, HORARIO_MATCHMAKING);
    planificador.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(planificador::detener));

    PlanificadorAvisosInactividad planificadorAvisos =
        new PlanificadorAvisosInactividad(gestorDonantes, HORARIO_AVISOS_INACTIVIDAD);
    planificadorAvisos.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(planificadorAvisos::detener));

    System.out.println("Microservicio Donaciones escuchando en " + urlBase);
  }

  private static void registrarManejoDeErrores(Javalin app) {
    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));

    app.exception(EventoYaAplicadoException.class, (e, ctx) ->
        ctx.status(409).json(new ErrorDTO(e.getMessage())));

    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));

    app.exception(ConflictoDeNegocioException.class, (e, ctx) ->
        ctx.status(409).json(new ErrorDTO(e.getMessage())));

    // OJO: NO mapeamos las transiciones imposibles a 409. Logistica trata el 409 como
    // "ya estaba aplicado, lo ignoro", asi que una transicion imposible respondida con 409
    // se comeria el error en silencio. Va a 422 (Unprocessable Entity) para que del otro
    // lado se convierta en IntegracionExternaException.
    app.exception(TransicionNoPermitidaException.class, (e, ctx) ->
        ctx.status(422).json(new ErrorDTO(e.getMessage())));

    // Fallas al hablar con Keycloak: 502 porque es un problema con un backend
    // externo, no con la request del cliente.
    app.exception(IntegracionConIdentidadException.class, (e, ctx) -> {
      e.printStackTrace();
      ctx.status(502).json(new ErrorDTO("Error al hablar con el proveedor de identidad."));
    });

    // Los 401 (UnauthorizedResponse) y 403 (ForbiddenResponse) del filtro de
    // seguridad son HttpResponseException, asi que caen aca y devuelven ErrorDTO.
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));

    app.exception(Exception.class, (e, ctx) -> {
      e.printStackTrace();
      ctx.status(500).json(new ErrorDTO("Error interno del servicio de Donaciones."));
    });
  }

  private static void sembrarSubcategorias(RepositorioSubcategorias repo) {
    Categoria mobiliario = new Categoria("Mobiliario");
    Categoria alimentos = new Categoria("Alimentos");
    Categoria vestimenta = new Categoria("Vestimenta");

    repo.guardar(new Subcategoria("Sillas", mobiliario, Unidades.UNIDADES));
    repo.guardar(new Subcategoria("Mesas", mobiliario, Unidades.UNIDADES));
    repo.guardar(new Subcategoria("Bancos escolares", mobiliario, Unidades.UNIDADES));
    repo.guardar(new Subcategoria("Fideos secos", alimentos, Unidades.KILOGRAMOS));
    repo.guardar(new Subcategoria("Tomate en tetrapak", alimentos, Unidades.UNIDADES));
    repo.guardar(new Subcategoria("Camperas de abrigo", vestimenta, Unidades.UNIDADES));
  }

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }
}