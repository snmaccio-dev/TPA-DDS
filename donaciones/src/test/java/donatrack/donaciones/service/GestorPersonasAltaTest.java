package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorPersonasAltaTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();
  private final RepositorioNotificaciones repoNotificaciones = new RepositorioNotificaciones();
  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();

  @Test
  void altaConPersonaExistenteQueYaTieneCuentaNoLlamaAKeycloakYAgregaElEmail() {
    String documento = DatosDeTest.documentoUnico();
    String emailVieja = DatosDeTest.emailUnico("ana-vieja");
    String emailNueva = DatosDeTest.emailUnico("ana-nueva");
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, emailVieja));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    repoUsuarios.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    PersonaHumana mismaPersonaMismoDocumento =
        new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    assertDoesNotThrow(() -> gestor.registrar(mismaPersonaMismoDocumento, emailNueva));

    assertTrue(proveedor.emailsCreados.isEmpty(),
        "No se tiene que llamar a Keycloak si el perfil ya tenia cuenta.");

    // Despues del gestor la transaccion esta cerrada: solo clear() para releer.
    entityManager().clear();
    PersonaHumana refrescada = (PersonaHumana) repoPersonas.buscarPorDocumento(documento)
        .orElseThrow();
    assertTrue(refrescada.getContactos().stream()
        .anyMatch(c -> c.getTipo() == TipoContacto.EMAIL
            && emailNueva.equals(c.getValor())),
        "El email nuevo tiene que haber quedado agregado como medio de contacto.");
  }

  @Test
  void altaConPersonaExistenteSinCuentaCreaLaCuenta() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("luis");
    PersonaHumana persona = new PersonaHumana("Luis", "Garcia", 40, documento, Genero.MASCULINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    entityManager().flush();
    entityManager().clear();

    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    PersonaHumana reintento =
        new PersonaHumana("Luis", "Garcia", 40, documento, Genero.MASCULINO);
    gestor.registrar(reintento, email);

    assertEquals(1, proveedor.emailsCreados.size());
    assertEquals(email, proveedor.emailsCreados.get(0));

    entityManager().clear();
    Donante refrescado = repoDonantes.buscarPorDocumento(documento).orElseThrow();
    assertTrue(repoUsuarios.existeParaDonante(refrescado));
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> emailsCreados = new ArrayList<>();
    private final String idFijo;

    ProveedorFalso() { this(null); }
    ProveedorFalso(String idFijo) { this.idFijo = idFijo; }

    @Override public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      emailsCreados.add(email);
      return idFijo != null ? idFijo : "kc-" + email;
    }

    @Override public void eliminarUsuario(String idExterno) {
      // no-op
    }
  }
}
