package donatrack.donaciones;

import donatrack.donaciones.controller.AsignacionesController;
import donatrack.donaciones.controller.DonacionesController;
import donatrack.donaciones.controller.DonantesController;
import donatrack.donaciones.controller.EntidadesBeneficiariasController;
import donatrack.donaciones.controller.NecesidadesController;
import donatrack.donaciones.controller.dto.ErrorDTO;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.infrastructure.notificacion.NotificadorEmail;
import donatrack.donaciones.infrastructure.notificacion.NotificadorSMS;
import donatrack.donaciones.infrastructure.notificacion.NotificadorWhatsApp;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.service.EventoYaAplicadoException;
import donatrack.donaciones.service.GestorAsignaciones;
import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.service.GestorDonantes;
import donatrack.donaciones.service.GestorEntidadesBeneficiarias;
import donatrack.donaciones.service.GestorNecesidades;
import donatrack.donaciones.service.GestorNotificaciones;
import donatrack.donaciones.service.GestorPersonas;
import donatrack.donaciones.service.PlanificadorAsignaciones;
import donatrack.donaciones.service.PlanificadorAvisosInactividad;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;

import java.time.LocalTime;
import java.util.List;

public class DonacionesApp {

  private static final int PUERTO = 8080;
  private static final LocalTime HORARIO_MATCHMAKING = LocalTime.of(3, 0);
  private static final LocalTime HORARIO_AVISOS_INACTIVIDAD = LocalTime.of(9, 0);

  public static void main(String[] args) {
    String urlBase = variable("DONACIONES_URL_BASE", "http://localhost:" + PUERTO);

    RepositorioPropuestas repositorioPropuestas = new RepositorioPropuestas();
    RepositorioSubcategorias repositorioSubcategorias = new RepositorioSubcategorias();
    sembrarSubcategorias(repositorioSubcategorias);

    RepositorioPersonas repositorioPersonas = RepositorioPersonas.getInstance();
    RepositorioEntidades repositorioEntidades = RepositorioEntidades.getInstance();
    RepositorioDonantes repositorioDonantes = new RepositorioDonantes();

    GestorNotificaciones gestorNotificaciones = new GestorNotificaciones(List.of(
        new NotificadorEmail(),
        new NotificadorSMS(),
        new NotificadorWhatsApp()
    ));
    GestorDonaciones gestorDonaciones = new GestorDonaciones(gestorNotificaciones, repositorioPropuestas);
    GestorAsignaciones gestorAsignaciones = new GestorAsignaciones(repositorioPropuestas);
    GestorEntidadesBeneficiarias gestorEntidades = new GestorEntidadesBeneficiarias();
    GestorNecesidades gestorNecesidades = new GestorNecesidades();
    GestorPersonas gestorPersonas = new GestorPersonas(new NotificadorEmail(), repositorioDonantes);
    GestorDonantes gestorDonantes = new GestorDonantes(repositorioDonantes, gestorNotificaciones);

    Javalin app = Javalin.create().start(PUERTO);

    new DonacionesController(gestorDonaciones, repositorioPersonas, repositorioSubcategorias).registrarRutas(app);
    new AsignacionesController(gestorAsignaciones, RepositorioDonaciones.getInstance(),
        repositorioEntidades, repositorioPersonas).registrarRutas(app);
    new EntidadesBeneficiariasController(gestorEntidades).registrarRutas(app);
    new NecesidadesController(gestorNecesidades, repositorioSubcategorias, repositorioEntidades).registrarRutas(app);
    new DonantesController(gestorPersonas, gestorDonantes).registrarRutas(app);

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

    app.exception(IllegalArgumentException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));

    // OJO: NO mapeamos IllegalStateException a 409. Logistica trata el 409 como
    // "ya estaba aplicado, lo ignoro", asi que una transicion imposible respondida con 409
    // se comeria el error en silencio. Va a 422 (Unprocessable Entity) para que del otro
    // lado se convierta en IntegracionExternaException.
    app.exception(IllegalStateException.class, (e, ctx) ->
        ctx.status(422).json(new ErrorDTO(e.getMessage())));
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