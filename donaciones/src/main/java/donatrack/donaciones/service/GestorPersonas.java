package donatrack.donaciones.service;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonantes;
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
    private final ProveedorDeIdentidad proveedor;

    public GestorPersonas(RepositorioDonantes repositorioDonantes,
                          RepositorioUsuarios repositorioUsuarios,
                          ProveedorDeIdentidad proveedor) {
        this.repositorioDonantes = repositorioDonantes;
        this.repositorioUsuarios = repositorioUsuarios;
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

    public void eliminar(String documento) {
        withTransaction(() ->
            repositorio.buscarPorDocumento(documento).ifPresent(repositorio::eliminar)
        );
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