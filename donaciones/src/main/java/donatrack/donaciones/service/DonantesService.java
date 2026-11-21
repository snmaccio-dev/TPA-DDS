package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.Enumerados;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DonantesService implements WithSimplePersistenceUnit {

    public static final int DIAS_SIN_INTERACCION = 20;

    private final RepositorioDonantes repositorio;
    private final RepositorioPersonas repositorioPersonas;
    private final RepositorioNotificaciones repositorioNotificaciones;
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioEntidades repositorioEntidades;
    private final NotificacionesService notificacionesService;
    private final UsuariosService usuariosService;

    public DonantesService(RepositorioDonantes repositorio,
                           RepositorioPersonas repositorioPersonas,
                           RepositorioNotificaciones repositorioNotificaciones,
                           RepositorioDonaciones repositorioDonaciones,
                           RepositorioEntidades repositorioEntidades,
                           NotificacionesService notificacionesService,
                           UsuariosService usuariosService) {
        this.repositorio = repositorio;
        this.repositorioPersonas = repositorioPersonas;
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioEntidades = repositorioEntidades;
        this.notificacionesService = notificacionesService;
        this.usuariosService = usuariosService;
    }

    public Donante registrarConPersonaNueva(Persona persona, String email) {
        if (repositorioPersonas.buscarPorDocumento(persona.getDocumento()).isPresent()) {
            throw new ConflictoDeNegocioException(
                "Ya existe una persona con ese documento o CUIT. Si se la quiere hacer donante, "
                    + "usa el alta con persona existente.");
        }
        return crearConCuenta(persona, email,
            usuariosService.obtenerCuenta(email, RolDeAcceso.DONANTE, true));
    }

    public Donante registrarConPersonaExistente(String documento, String email) {
        Persona persona = repositorioPersonas.buscarPorDocumento(documento)
            .orElseThrow(() -> new RecursoInexistenteException(
                "No existe la persona con documento " + documento));
        if (repositorio.buscarPorPersona(persona).isPresent()) {
            throw new ConflictoDeNegocioException("La persona ya es donante.");
        }

        UsuariosService.CuentaObtenida cuenta =
            usuariosService.obtenerCuenta(email, RolDeAcceso.DONANTE, true);
        String keycloakId = cuenta.keycloakId();
        Donante[] creado = new Donante[1];
        try {
            withTransaction(() -> {
                if (!persona.tieneContacto(TipoContacto.EMAIL, email)) {
                    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
                }
                creado[0] = new Donante(persona);
                repositorio.guardar(creado[0]);
                usuariosService.vincular(keycloakId, creado[0]);
            });
        } catch (RuntimeException e) {
            if (cuenta.creada()) {
                usuariosService.compensar(keycloakId);
            }
            throw e;
        }
        return creado[0];
    }

    public Donante registrarPublico(Persona persona, String email, String contrasena) {
        UsuariosService.validarCredenciales(email, contrasena);
        if (persona == null) {
            throw new DatosInvalidosException("Los datos de la persona son obligatorios.");
        }
        if (repositorioPersonas.buscarPorDocumento(persona.getDocumento()).isPresent()) {
            throw new ConflictoDeNegocioException(UsuariosService.MENSAJE_PERSONA_DUPLICADA);
        }
        // El registro publico siempre crea la cuenta: si el mail ya existe, el proveedor
        // responde 409 y la persona tiene que ingresar con esa cuenta.
        return crearConCuenta(persona, email, new UsuariosService.CuentaObtenida(
            usuariosService.crearEnProveedor(email, contrasena, RolDeAcceso.DONANTE), true));
    }

    private Donante crearConCuenta(Persona persona, String email,
                                   UsuariosService.CuentaObtenida cuenta) {
        String keycloakId = cuenta.keycloakId();
        Donante[] creado = new Donante[1];
        try {
            withTransaction(() -> {
                creado[0] = crearPersonaYDonante(persona, email);
                usuariosService.vincular(keycloakId, creado[0]);
            });
        } catch (RuntimeException e) {
            if (cuenta.creada()) {
                usuariosService.compensar(keycloakId);
            }
            throw e;
        }
        return creado[0];
    }

    private Donante crearPersonaYDonante(Persona persona, String email) {
        persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
        repositorioPersonas.guardar(persona);
        Donante donante = new Donante(persona);
        repositorio.guardar(donante);
        return donante;
    }

    public void eliminar(String documento) {
        Donante donante = repositorio.buscarPorDocumento(documento)
            .orElseThrow(() -> new RecursoInexistenteException(
                "No existe un donante con documento " + documento));

        if (repositorioDonaciones.tieneDonacionesDe(donante)) {
            throw new ConflictoDeNegocioException(
                "El donante tiene donaciones registradas y no puede eliminarse "
                    + "para conservar la trazabilidad.");
        }

        String[] keycloakIdAEliminar = new String[1];

        withTransaction(() -> {
            Persona persona = donante.getPersona();
            repositorioNotificaciones.eliminarDeDonante(donante);
            usuariosService.desvincular(donante).ifPresent(id -> keycloakIdAEliminar[0] = id);
            repositorio.eliminar(donante);
            if (!personaUsadaPorBeneficiaria(persona)) {
                repositorioPersonas.eliminar(persona);
            }
        });

        if (keycloakIdAEliminar[0] != null) {
            usuariosService.eliminarEnProveedor(keycloakIdAEliminar[0]);
        }
    }

    public Persona modificar(String documento, DatosDonante datos) {
        if (datos == null) {
            throw new DatosInvalidosException("Los datos del donante son obligatorios.");
        }
        Persona[] modificada = new Persona[1];
        try {
            withTransaction(() -> {
                modificada[0] = buscarPorDocumento(documento);
                aplicar(modificada[0], datos);
            });
        } catch (RuntimeException e) {
            entityManager().clear();
            throw e;
        }
        return modificada[0];
    }

    private static void aplicar(Persona persona, DatosDonante datos) {
        TipoContacto predeterminado = Enumerados.desde(
            TipoContacto.class, datos.contactoPredeterminado(), "contactoPredeterminado");
        if (persona instanceof PersonaHumana humana) {
            humana.actualizarDatos(datos.nombre(), datos.apellido(), datos.edad(),
                Enumerados.desde(Genero.class, datos.genero(), "genero"));
        } else if (persona instanceof PersonaJuridica juridica) {
            juridica.actualizarDatos(datos.razonSocial(),
                Enumerados.desde(TipoOrganizacion.class, datos.tipoOrganizacion(), "tipoOrganizacion"),
                datos.rubro());
        }
        persona.setDireccion(datos.direccion() == null || datos.direccion().isBlank()
            ? null
            : datos.direccion().trim());
        persona.reemplazarContacto(TipoContacto.TELEFONO, datos.telefono());
        persona.reemplazarContacto(TipoContacto.WHATSAPP, datos.whatsapp());
        persona.elegirContactoPredeterminado(predeterminado);
    }

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    // Listado paginado para el panel de administracion. pagina empieza en 1; un tamanio
    // mayor al maximo se recorta. busqueda (opcional) filtra por nombre o razon social,
    // documento o CUIT, o mail, sin distinguir mayusculas.
    public Pagina<Persona> pagina(String busqueda, int pagina, int tamanio) {
        if (pagina < 1) {
            throw new DatosInvalidosException("La pagina empieza en 1.");
        }
        if (tamanio < 1) {
            throw new DatosInvalidosException("El tamanio de pagina tiene que ser mayor a 0.");
        }
        int tamanioEfectivo = Math.min(tamanio, TAMANIO_MAXIMO);

        long total = repositorio.contar(busqueda);
        List<Persona> personas = total == 0
            ? List.of()
            : repositorio.pagina(busqueda, pagina, tamanioEfectivo).stream()
                .map(Donante::getPersona)
                .toList();

        return new Pagina<>(personas, total, pagina, tamanioEfectivo);
    }

    public Persona buscarPorDocumento(String documento) {
        return repositorio.buscarPorDocumento(documento)
            .map(Donante::getPersona)
            .orElseThrow(() -> new RecursoInexistenteException(
                "No existe un donante con documento " + documento));
    }

    public Donante buscarDonantePorDocumento(String documento) {
        return repositorio.buscarPorDocumento(documento)
            .orElseGet(() -> {
                repositorioPersonas.buscarPorDocumento(documento)
                    .orElseThrow(() -> new RecursoInexistenteException(
                        "No existe la persona con documento " + documento
                    ));
                throw new DatosInvalidosException(
                    "La persona con documento " + documento + " no tiene el rol Donante."
                );
            });
    }

    public List<Donante> inactivos() {
        LocalDate limite = LocalDate.now().minusDays(DIAS_SIN_INTERACCION);
        return repositorio.queRequierenAvisoDeInactividad(limite);
    }

    public ResultadoCorrida avisarInactivos() {
        int avisados = 0;
        List<String> fallidos = new ArrayList<>();
        for (Donante donante : inactivos()) {
            Notificacion notificacion =
                notificacionesService.notificarAusenciaPlataforma(donante);
            if (notificacion.getEstadoEnvio() == EstadoEnvio.ENVIADA) {
                withTransaction(donante::registrarAvisoDeInactividad);
                avisados++;
            } else {
                fallidos.add(donante.getPersona().getDocumento() + ": "
                    + (notificacion.getDetalleError() == null
                        ? "no se pudo enviar la notificacion"
                        : notificacion.getDetalleError()));
            }
        }
        return new ResultadoCorrida(avisados, fallidos);
    }

    private boolean personaUsadaPorBeneficiaria(Persona persona) {
        if (!(persona instanceof PersonaJuridica juridica)) {
            return false;
        }
        return repositorioEntidades.buscarPorCuit(juridica.getCuit()).isPresent();
    }
}
