package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;
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
        return crearConCuenta(persona, email, null);
    }

    public Donante registrarConPersonaExistente(String documento, String email) {
        Persona persona = repositorioPersonas.buscarPorDocumento(documento)
            .orElseThrow(() -> new RecursoInexistenteException(
                "No existe la persona con documento " + documento));
        if (repositorio.buscarPorPersona(persona).isPresent()) {
            throw new ConflictoDeNegocioException("La persona ya es donante.");
        }

        String keycloakId = usuariosService.crearEnProveedor(email, null, RolDeAcceso.DONANTE);
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
            usuariosService.compensar(keycloakId);
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
        return crearConCuenta(persona, email, contrasena);
    }

    private Donante crearConCuenta(Persona persona, String email, String contrasena) {
        String keycloakId = usuariosService.crearEnProveedor(email, contrasena, RolDeAcceso.DONANTE);
        Donante[] creado = new Donante[1];
        try {
            withTransaction(() -> {
                creado[0] = crearPersonaYDonante(persona, email);
                usuariosService.vincular(keycloakId, creado[0]);
            });
        } catch (RuntimeException e) {
            usuariosService.compensar(keycloakId);
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

    public List<Persona> todos() {
        return repositorio.todos().stream()
            .map(Donante::getPersona)
            .toList();
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
