package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

// Alta desde el deposito (la hace un administrador). La contrasena no se genera
// aca: creamos la cuenta sin contrasena y Keycloak le manda al usuario el
// correo UPDATE_PASSWORD para que la elija.
public class GestorPersonas implements WithSimplePersistenceUnit {

    private final RepositorioPersonas repositorio =
        RepositorioPersonas.getInstance();

    private final RepositorioDonantes repositorioDonantes;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioNotificaciones repositorioNotificaciones;
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioEntidades repositorioEntidades;
    private final ProveedorDeIdentidad proveedor;

    public GestorPersonas(RepositorioDonantes repositorioDonantes,
                          RepositorioUsuarios repositorioUsuarios,
                          RepositorioNotificaciones repositorioNotificaciones,
                          RepositorioDonaciones repositorioDonaciones,
                          RepositorioEntidades repositorioEntidades,
                          ProveedorDeIdentidad proveedor) {
        this.repositorioDonantes = repositorioDonantes;
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioEntidades = repositorioEntidades;
        this.proveedor = proveedor;
    }

    public void registrar(Persona persona, String email) {
        Optional<Persona> existente = repositorio.buscarPorDocumento(persona.getDocumento());

        if (existente.isPresent()) {
            Persona actual = existente.get();
            Donante donante = asegurarDonanteYContacto(actual, email);
            asegurarCuenta(donante, email);
            return;
        }

        // El Donante lo creamos dentro de esta misma transaccion, asi que no
        // puede tener cuenta previa: alcanza con el unique de la base como red
        // de seguridad. No metemos un chequeo defensivo adentro del
        // withTransaction para no dejar la persona a medio commitear si tiraramos
        // 409 desde ahi.
        String keycloakId = proveedor.crearUsuarioSinContrasena(email, RolDeAcceso.DONANTE);
        try {
            withTransaction(() -> {
                persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
                repositorio.guardar(persona);
                Donante donante = new Donante(persona);
                repositorioDonantes.guardar(donante);
                repositorioUsuarios.guardar(Usuario.deDonante(keycloakId, donante));
            });
        } catch (RuntimeException e) {
            intentarEliminar(keycloakId);
            throw e;
        }
    }

    private Donante asegurarDonanteYContacto(Persona actual, String email) {
        Donante[] resultado = new Donante[1];
        withTransaction(() -> {
            if (!actual.tieneContacto(TipoContacto.EMAIL, email)) {
                actual.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
            }
            Donante donante = repositorioDonantes.buscarPorPersona(actual).orElseGet(() -> {
                Donante nuevo = new Donante(actual);
                repositorioDonantes.guardar(nuevo);
                return nuevo;
            });
            resultado[0] = donante;
        });
        return resultado[0];
    }

    private void asegurarCuenta(Donante donante, String email) {
        // Cada perfil tiene una unica cuenta: si ya hay una, la reutilizamos
        // tal cual (no volvemos a crearla en Keycloak, ni la pisamos). El
        // flujo del admin incluye confirmar que el donante ya cuenta con un
        // usuario, asi que esto no es un error.
        if (repositorioUsuarios.existeParaDonante(donante)) {
            return;
        }
        String keycloakId = proveedor.crearUsuarioSinContrasena(email, RolDeAcceso.DONANTE);
        try {
            withTransaction(() ->
                repositorioUsuarios.guardar(Usuario.deDonante(keycloakId, donante))
            );
        } catch (RuntimeException e) {
            intentarEliminar(keycloakId);
            throw e;
        }
    }

    public Persona buscarPorDocumento(String documento) {
        return repositorio.buscarPorDocumento(documento)
            .orElseThrow(() ->
                new RecursoInexistenteException(
                    "No existe una persona con documento " + documento
                ));
    }

    public Optional<Persona> buscarPorEmail(String email) {
        return repositorio.buscarPorEmail(email);
    }

    public List<Persona> todos() {
        return repositorio.todos();
    }

    // Borrado en cascada de un donante. El contrato es:
    // 1. 404 si no existe un donante con ese documento.
    // 2. 409 si tiene donaciones registradas (conservamos trazabilidad).
    // 3. Dentro de una unica transaccion: notificaciones, usuario, donante y
    //    persona (la persona solo si no existe una Beneficiaria que la use).
    // 4. Despues del commit, borramos la cuenta en Keycloak. Si Keycloak falla
    //    se loguea y sigue: la cuenta queda sin perfil y el filtro la rechaza.
    public void eliminar(String documento) {
        Donante donante = repositorioDonantes.buscarPorDocumento(documento)
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
            Optional<Usuario> usuario = repositorioUsuarios.deDonante(donante);
            usuario.ifPresent(u -> {
                keycloakIdAEliminar[0] = u.getKeycloakId();
                repositorioUsuarios.eliminar(u);
            });
            repositorioDonantes.eliminar(donante);
            if (!personaUsadaPorBeneficiaria(persona)) {
                repositorio.eliminar(persona);
            }
        });

        if (keycloakIdAEliminar[0] != null) {
            intentarEliminar(keycloakIdAEliminar[0]);
        }
    }

    private boolean personaUsadaPorBeneficiaria(Persona persona) {
        // Solo una PersonaJuridica puede tener Beneficiaria.
        if (!(persona instanceof PersonaJuridica juridica)) {
            return false;
        }
        return repositorioEntidades.buscarPorCuit(juridica.getCuit()).isPresent();
    }

    private void intentarEliminar(String keycloakId) {
        try {
            proveedor.eliminarUsuario(keycloakId);
        } catch (RuntimeException ignorado) {
            System.err.println("[DONANTES] Rollback en Keycloak fallo para " + keycloakId
                + ": " + ignorado.getMessage());
        }
    }
}
