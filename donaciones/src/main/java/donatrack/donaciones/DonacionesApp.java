package donatrack.donaciones;

import donatrack.donaciones.controller.AdministradoresController;
import donatrack.donaciones.controller.AsignacionesController;
import donatrack.donaciones.controller.DonacionesController;
import donatrack.donaciones.controller.DonantesController;
import donatrack.donaciones.controller.EntidadesBeneficiariasController;
import donatrack.donaciones.controller.ImportacionesController;
import donatrack.donaciones.controller.NecesidadesController;
import donatrack.donaciones.controller.SubcategoriasController;
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
import donatrack.donaciones.domain.donacion.SegmentadorDonaciones;
import donatrack.donaciones.domain.donacion.asignacion.CompatibilidadSemantica;
import donatrack.donaciones.domain.donacion.asignacion.PrioridadSubatendidos;
import donatrack.donaciones.infrastructure.identidad.ProveedorDeIdentidadKeycloak;
import donatrack.donaciones.infrastructure.notificacion.NotificadorEmail;
import donatrack.donaciones.infrastructure.notificacion.NotificadorSMS;
import donatrack.donaciones.infrastructure.notificacion.NotificadorWhatsApp;
import donatrack.donaciones.jobs.PlanificadorAsignaciones;
import donatrack.donaciones.jobs.PlanificadorAvisosInactividad;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.infrastructure.importacion.LectorCsvDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioImportaciones;
import donatrack.donaciones.repository.RepositorioNecesidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.IntegracionConIdentidadException;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.BandejaDeNotificacionesService;
import donatrack.donaciones.service.EventoYaAplicadoException;
import donatrack.donaciones.service.ImportacionDonantesService;
import donatrack.donaciones.service.AsignacionesService;
import donatrack.donaciones.service.DonacionesService;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import donatrack.donaciones.service.NecesidadesService;
import donatrack.donaciones.service.NotificacionesService;
import donatrack.donaciones.service.PersonasService;
import donatrack.donaciones.service.RecursoInexistenteException;
import donatrack.donaciones.service.UsuariosService;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.SizeUnit;
import io.javalin.http.HttpResponseException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DonacionesApp {

  private static final int PUERTO = 8080;
  private static final int TAMANIO_MAXIMO_ARCHIVO_MB = 10;
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
    boolean importacionEnviaMails =
        Boolean.parseBoolean(variable("IMPORTACION_ENVIAR_MAILS", "false"));

    RepositorioPropuestas repositorioPropuestas = new RepositorioPropuestas();
    RepositorioSubcategorias repositorioSubcategorias = new RepositorioSubcategorias();
    sembrarSubcategorias(repositorioSubcategorias);

    RepositorioPersonas repositorioPersonas = RepositorioPersonas.getInstance();
    RepositorioEntidades repositorioEntidades = RepositorioEntidades.getInstance();
    RepositorioDonaciones repositorioDonaciones = RepositorioDonaciones.getInstance();
    RepositorioNecesidades repositorioNecesidades = RepositorioNecesidades.getInstance();
    RepositorioDonantes repositorioDonantes = new RepositorioDonantes();
    RepositorioUsuarios repositorioUsuarios = new RepositorioUsuarios();
    RepositorioNotificaciones repositorioNotificaciones = new RepositorioNotificaciones();

    ProveedorDeIdentidad proveedor = new ProveedorDeIdentidadKeycloak(
        keycloakUrl, keycloakRealm, keycloakClientId, keycloakClientSecret);

    VerificadorDeTokens verificador =
        new VerificadorDeTokens(keycloakUrl, keycloakRealm, keycloakIssuer);
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repositorioUsuarios);

    NotificacionesService notificacionesService = new NotificacionesService(List.of(
        new NotificadorEmail(),
        new NotificadorSMS(),
        new NotificadorWhatsApp()
    ), repositorioNotificaciones);
    BandejaDeNotificacionesService bandejaDeNotificaciones =
        new BandejaDeNotificacionesService(repositorioNotificaciones);
    UsuariosService usuariosService = new UsuariosService(proveedor, repositorioUsuarios);
    DonacionesService donacionesService = new DonacionesService(
        repositorioDonaciones,
        repositorioSubcategorias,
        repositorioPropuestas,
        repositorioNotificaciones,
        new SegmentadorDonaciones(),
        notificacionesService,
        urlWeb + "/seguimiento-donante",
        urlWeb + "/seguimiento-beneficiario"
    );
    AsignacionesService asignacionesService = new AsignacionesService(
        new CompatibilidadSemantica(),
        new PrioridadSubatendidos(),
        repositorioDonaciones,
        repositorioEntidades,
        repositorioPropuestas,
        donacionesService);
    EntidadesBeneficiariasService entidadesService = new EntidadesBeneficiariasService(
        repositorioEntidades,
        repositorioPersonas,
        repositorioNotificaciones,
        repositorioNecesidades,
        repositorioPropuestas,
        repositorioDonaciones,
        repositorioDonantes,
        usuariosService);
    NecesidadesService necesidadesService =
        new NecesidadesService(repositorioNecesidades, donacionesService);
    PersonasService personasService = new PersonasService(repositorioPersonas);
    DonantesService donantesService = new DonantesService(
        repositorioDonantes,
        repositorioPersonas,
        repositorioNotificaciones,
        repositorioDonaciones,
        repositorioEntidades,
        notificacionesService,
        usuariosService);
    ExecutorService ejecutorImportaciones = Executors.newSingleThreadExecutor(tarea -> {
      Thread hilo = new Thread(tarea, "importacion-donantes");
      hilo.setDaemon(true);
      return hilo;
    });
    ImportacionDonantesService importacionService = new ImportacionDonantesService(
        new LectorCsvDonantes(),
        new RepositorioImportaciones(),
        repositorioPersonas,
        repositorioDonantes,
        repositorioUsuarios,
        usuariosService,
        ejecutorImportaciones,
        importacionEnviaMails);

    Javalin app = Javalin.create(DonacionesApp::configurar).start(PUERTO);

    // El filtro se registra antes que las rutas: todas las rutas tienen
    // que declarar sus roles y el filtro las rechaza si no lo hacen.
    filtro.registrarEn(app);

    new Rutas(
        new RegistroController(donantesService, entidadesService),
        new UsuariosController(),
        new AdministradoresController(usuariosService),
        new DonacionesController(donacionesService, donantesService),
        new AsignacionesController(asignacionesService),
        new DonantesController(donantesService),
        new ImportacionesController(importacionService),
        new EntidadesBeneficiariasController(entidadesService, personasService),
        new NecesidadesController(necesidadesService, entidadesService),
        new SubcategoriasController(donacionesService),
        new NotificacionesController(bandejaDeNotificaciones)
    ).registrarEn(app);

    registrarManejoDeErrores(app);

    // Horarios configurables con el mismo formato HH:mm que PLANIFICACION_HORA de Logistica.
    PlanificadorAsignaciones planificador = new PlanificadorAsignaciones(
        asignacionesService, horario("ASIGNACIONES_HORA", HORARIO_MATCHMAKING));
    planificador.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(planificador::detener));

    PlanificadorAvisosInactividad planificadorAvisos =
        new PlanificadorAvisosInactividad(
            donantesService, horario("AVISOS_INACTIVIDAD_HORA", HORARIO_AVISOS_INACTIVIDAD));
    planificadorAvisos.iniciar();
    Runtime.getRuntime().addShutdownHook(new Thread(planificadorAvisos::detener));
    Runtime.getRuntime().addShutdownHook(new Thread(ejecutorImportaciones::shutdown));

    System.out.println("Microservicio Donaciones escuchando en " + urlBase);
  }

  static void configurar(JavalinConfig config) {
    config.jetty.multipartConfig.maxFileSize(TAMANIO_MAXIMO_ARCHIVO_MB, SizeUnit.MB);
    config.jetty.multipartConfig.maxTotalRequestSize(TAMANIO_MAXIMO_ARCHIVO_MB, SizeUnit.MB);
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

  /*
  private static void sembrarSubcategorias(RepositorioSubcategorias repo) {
    // Sin withTransaction el persist se queda en el contexto de persistencia y no se
    // commitea nunca: el catalogo quedaba vacio y cualquier alta de donacion fallaba
    // con "No existe la subcategoria". El resto de las escrituras del modulo ya pasan
    // por el withTransaction de su service; esta sembrada corre suelta desde el main.
    // La guarda es necesaria porque esto se ejecuta en cada arranque: sin ella, apenas
    // el commit empieza a funcionar, cada reinicio duplicaria las seis filas.
    if (!repo.todas().isEmpty()) {
      return;
    }

    repo.withTransaction(() -> {
      Categoria mobiliario = new Categoria("Mobiliario");
      Categoria alimentos = new Categoria("Alimentos");
      Categoria vestimenta = new Categoria("Vestimenta");

      // Las categorias se persisten aparte: el @ManyToOne de Subcategoria.categoria no
      // tiene cascade, asi que guardar la subcategoria sola deja una referencia a una
      // instancia transitoria y el flush del commit falla.
      repo.persist(mobiliario);
      repo.persist(alimentos);
      repo.persist(vestimenta);

      repo.guardar(new Subcategoria("Sillas", mobiliario, Unidades.UNIDADES));
      repo.guardar(new Subcategoria("Mesas", mobiliario, Unidades.UNIDADES));
      repo.guardar(new Subcategoria("Bancos escolares", mobiliario, Unidades.UNIDADES));
      repo.guardar(new Subcategoria("Fideos secos", alimentos, Unidades.KILOGRAMOS));
      repo.guardar(new Subcategoria("Tomate en tetrapak", alimentos, Unidades.UNIDADES));
      repo.guardar(new Subcategoria("Camperas de abrigo", vestimenta, Unidades.UNIDADES));
    });
  }
  */

  private static String variable(String nombre, String valorPorDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? valorPorDefecto : valor;
  }

  private static LocalTime horario(String nombre, LocalTime valorPorDefecto) {
    String texto = variable(nombre, null);
    if (texto == null) {
      return valorPorDefecto;
    }
    try {
      return LocalTime.parse(texto.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "El horario de " + nombre + " '" + texto + "' no tiene el formato HH:mm.");
    }
  }



    private static void sembrarSubcategorias(RepositorioSubcategorias repo) {
        repo.withTransaction(() -> {

            // Crear o recuperar una categoría existente
            Categoria mobiliario = obtenerOCrearCategoria(repo, "Mobiliario");
            Categoria alimentos = obtenerOCrearCategoria(repo, "Alimentos");
            Categoria vestimenta = obtenerOCrearCategoria(repo, "Vestimenta");
            Categoria electronica = obtenerOCrearCategoria(repo, "Electrónica");
            Categoria electrodomesticos = obtenerOCrearCategoria(repo, "Electrodomésticos");
            Categoria juguetes = obtenerOCrearCategoria(repo, "Juguetes");
            Categoria educacion = obtenerOCrearCategoria(repo, "Libros y educación");
            Categoria higiene = obtenerOCrearCategoria(repo, "Higiene personal");
            Categoria limpieza = obtenerOCrearCategoria(repo, "Limpieza");
            Categoria deportes = obtenerOCrearCategoria(repo, "Deportes");

            // MOBILIARIO
            agregarSubcategoria(repo, "Sillas", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Mesas", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Bancos escolares", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Escritorios", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Armarios", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Estanterias", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Camas", mobiliario, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Colchones", mobiliario, Unidades.UNIDADES);

            // ALIMENTOS
            agregarSubcategoria(repo, "Fideos secos", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Arroz", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Harina", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Leche", alimentos, Unidades.LITROS);
            agregarSubcategoria(repo, "Aceite", alimentos, Unidades.LITROS);
            agregarSubcategoria(repo, "Azucar", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Yerba", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Legumbres", alimentos, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Conservas", alimentos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Tomate en tetrapak", alimentos, Unidades.UNIDADES);

            // VESTIMENTA
            agregarSubcategoria(repo, "Camperas", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Remeras", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Pantalones", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Buzos", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Calzado", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Medias", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Ropa de bebe", vestimenta, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Uniformes escolares", vestimenta, Unidades.UNIDADES);

            // ELECTRONICA
            agregarSubcategoria(repo, "Computadoras", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Notebooks", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Celulares", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Tablets", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Monitores", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Televisores", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Impresoras", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Teclados", electronica, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Mouse", electronica, Unidades.UNIDADES);

            // ELECTRODOMESTICOS
            agregarSubcategoria(repo, "Heladeras", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Cocinas", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Microondas", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Lavarropas", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Ventiladores", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Estufas", electrodomesticos, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Pavas electricas", electrodomesticos, Unidades.UNIDADES);

            // JUGUETES
            agregarSubcategoria(repo, "Peluches", juguetes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Muñecas", juguetes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Juegos de mesa", juguetes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Bloques de construccion", juguetes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Juguetes educativos", juguetes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Rompecabezas", juguetes, Unidades.UNIDADES);

            // LIBROS Y EDUCACION
            agregarSubcategoria(repo, "Libros de literatura", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Manuales escolares", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Cuadernos", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Carpetas", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Diccionarios", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Utiles escolares", educacion, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Mochilas", educacion, Unidades.UNIDADES);

            // HIGIENE PERSONAL
            agregarSubcategoria(repo, "Jabones", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Champu", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Acondicionador", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Pasta dental", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Cepillos de dientes", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Toallas femeninas", higiene, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Pañales", higiene, Unidades.UNIDADES);

            // LIMPIEZA
            agregarSubcategoria(repo, "Detergente", limpieza, Unidades.LITROS);
            agregarSubcategoria(repo, "Lavandina", limpieza, Unidades.LITROS);
            agregarSubcategoria(repo, "Desinfectante", limpieza, Unidades.LITROS);
            agregarSubcategoria(repo, "Jabon para ropa", limpieza, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Esponjas", limpieza, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Escobas", limpieza, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Trapos de piso", limpieza, Unidades.UNIDADES);

            // DEPORTES
            agregarSubcategoria(repo, "Pelotas", deportes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Raquetas", deportes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Bicicletas", deportes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Colchonetas", deportes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Pesas", deportes, Unidades.KILOGRAMOS);
            agregarSubcategoria(repo, "Redes deportivas", deportes, Unidades.UNIDADES);
            agregarSubcategoria(repo, "Indumentaria deportiva", deportes, Unidades.UNIDADES);
        });
    }

    private static Categoria obtenerOCrearCategoria(
        RepositorioSubcategorias repo, String nombre) {

        return repo.buscarCategoriaPorNombre(nombre)
            .orElseGet(() -> {
                Categoria categoria = new Categoria(nombre);
                repo.guardarCategoria(categoria);
                return categoria;
            });
    }

    private static void agregarSubcategoria(
        RepositorioSubcategorias repo,
        String nombre,
        Categoria categoria,
        Unidades unidad) {

        if (repo.buscarPorNombre(nombre).isEmpty()) {
            repo.guardar(new Subcategoria(nombre, categoria, unidad));
        }
    }


}