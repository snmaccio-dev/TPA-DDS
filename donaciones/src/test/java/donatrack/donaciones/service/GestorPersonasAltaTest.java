package donatrack.donaciones.service;

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
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana.vieja@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    repoUsuarios.guardar(Usuario.deDonante("kc-ana", donante));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    PersonaHumana mismaPersonaMismoDocumento =
        new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);
    assertDoesNotThrow(() -> gestor.registrar(mismaPersonaMismoDocumento, "ana.nueva@mail.com"));

    assertTrue(proveedor.emailsCreados.isEmpty(),
        "No se tiene que llamar a Keycloak si el perfil ya tenia cuenta.");

    entityManager().clear();
    PersonaHumana refrescada = (PersonaHumana) repoPersonas.buscarPorDocumento("11111111")
        .orElseThrow();
    assertTrue(refrescada.getContactos().stream()
        .anyMatch(c -> c.getTipo() == TipoContacto.EMAIL
            && "ana.nueva@mail.com".equals(c.getValor())),
        "El email nuevo tiene que haber quedado agregado como medio de contacto.");
  }

  @Test
  void altaConPersonaExistenteSinCuentaCreaLaCuenta() {
    PersonaHumana persona = new PersonaHumana("Luis", "Garcia", 40, "22222222", Genero.MASCULINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "luis@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso("kc-luis");
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    PersonaHumana reintento =
        new PersonaHumana("Luis", "Garcia", 40, "22222222", Genero.MASCULINO);
    gestor.registrar(reintento, "luis@mail.com");

    assertEquals(1, proveedor.emailsCreados.size());
    assertEquals("luis@mail.com", proveedor.emailsCreados.get(0));

    entityManager().clear();
    Donante refrescado = repoDonantes.buscarPorDocumento("22222222").orElseThrow();
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
