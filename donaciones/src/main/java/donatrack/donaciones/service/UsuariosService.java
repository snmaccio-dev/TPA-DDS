package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.PoliticaDeContrasenas;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UsuariosService {

    static final String MENSAJE_PERSONA_DUPLICADA =
        "Ya existe una persona registrada con ese documento. "
            + "Si te dieron de alta en el deposito, revisa el correo de bienvenida.";

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosService.class);

    private final ProveedorDeIdentidad proveedor;
    private final RepositorioUsuarios repositorioUsuarios;

    public UsuariosService(ProveedorDeIdentidad proveedor, RepositorioUsuarios repositorioUsuarios) {
        this.proveedor = proveedor;
        this.repositorioUsuarios = repositorioUsuarios;
    }

    public String crearEnProveedor(String email, String contrasena, RolDeAcceso rol) {
        if (contrasena == null) {
            return proveedor.crearUsuarioSinContrasena(email, rol);
        }
        return proveedor.crearUsuario(email, contrasena, rol);
    }

    public String crearSinContrasena(String email, RolDeAcceso rol, boolean enviarMail) {
        return proveedor.crearUsuarioSinContrasena(email, rol, enviarMail);
    }

    public Optional<String> buscarIdPorEmail(String email) {
        return proveedor.buscarIdPorEmail(email);
    }

    public void vincular(String keycloakId, Donante donante) {
        repositorioUsuarios.guardar(Usuario.deDonante(keycloakId, donante));
    }

    public void vincular(String keycloakId, Beneficiaria beneficiaria) {
        repositorioUsuarios.guardar(Usuario.deBeneficiaria(keycloakId, beneficiaria));
    }

    public void compensar(String keycloakId) {
        eliminarSinPropagar(keycloakId, "No se pudo compensar el alta en el proveedor");
    }

    public void eliminarEnProveedor(String keycloakId) {
        eliminarSinPropagar(keycloakId, "No se pudo eliminar la cuenta en el proveedor");
    }

    public Optional<String> desvincular(Donante donante) {
        Optional<Usuario> usuario = repositorioUsuarios.deDonante(donante);
        usuario.ifPresent(repositorioUsuarios::eliminar);
        return usuario.map(Usuario::getKeycloakId);
    }

    public Optional<String> desvincular(Beneficiaria beneficiaria) {
        Optional<Usuario> usuario = repositorioUsuarios.deBeneficiaria(beneficiaria);
        usuario.ifPresent(repositorioUsuarios::eliminar);
        return usuario.map(Usuario::getKeycloakId);
    }

    public String crearAdministrador(String email, String contrasena) {
        if (email == null || email.isBlank()) {
            throw new DatosInvalidosException("El email del administrador es obligatorio.");
        }
        PoliticaDeContrasenas.validar(contrasena);
        return crearEnProveedor(email, contrasena, RolDeAcceso.ADMINISTRADOR);
    }

    static void validarCredenciales(String email, String contrasena) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new DatosInvalidosException("El email es obligatorio y debe ser valido.");
        }
        PoliticaDeContrasenas.validar(contrasena);
    }

    private void eliminarSinPropagar(String keycloakId, String contexto) {
        try {
            proveedor.eliminarUsuario(keycloakId);
        } catch (RuntimeException e) {
            LOG.warn("{} para {}: {}", contexto, keycloakId, e.getMessage());
        }
    }
}
