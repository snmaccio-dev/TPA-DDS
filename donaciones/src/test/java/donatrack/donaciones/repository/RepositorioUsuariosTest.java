package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioUsuariosTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repositorio = new RepositorioUsuarios();

  @Test
  void guardarYBuscarPorKeycloakId() {
    Donante donante = donantePersistido("Ana", "12345678");

    Usuario usuario = Usuario.deDonante("kc-111", donante);
    repositorio.guardar(usuario);

    entityManager().flush();
    entityManager().clear();

    Optional<Usuario> recuperado = repositorio.buscarPorKeycloakId("kc-111");

    assertTrue(recuperado.isPresent());
    assertEquals("kc-111", recuperado.get().getKeycloakId());
    assertEquals(donante.getId(), recuperado.get().getDonante().getId());
  }

  @Test
  void variosUsuariosPuedenApuntarAlMismoDonante() {
    Donante donante = donantePersistido("Luis", "22222222");

    repositorio.guardar(Usuario.deDonante("kc-A", donante));
    repositorio.guardar(Usuario.deDonante("kc-B", donante));

    entityManager().flush();
    entityManager().clear();

    assertTrue(repositorio.existeParaDonante(donante));
    assertTrue(repositorio.buscarPorKeycloakId("kc-A").isPresent());
    assertTrue(repositorio.buscarPorKeycloakId("kc-B").isPresent());
  }

  @Test
  void existeParaDonanteDistingueDonantes() {
    Donante conUsuario = donantePersistido("Maria", "33333333");
    Donante sinUsuario = donantePersistido("Pedro", "44444444");

    repositorio.guardar(Usuario.deDonante("kc-M", conUsuario));

    entityManager().flush();
    entityManager().clear();

    // Refresca los donantes despues del clear.
    Donante refrescadoConUsuario = entityManager().find(Donante.class, conUsuario.getId());
    Donante refrescadoSinUsuario = entityManager().find(Donante.class, sinUsuario.getId());

    assertTrue(repositorio.existeParaDonante(refrescadoConUsuario));
    assertFalse(repositorio.existeParaDonante(refrescadoSinUsuario));
  }

  @Test
  void existeParaBeneficiariaDistingueBeneficiarias() {
    Beneficiaria conUsuario = beneficiariaPersistida("30-11111111-1");
    Beneficiaria sinUsuario = beneficiariaPersistida("30-22222222-2");

    repositorio.guardar(Usuario.deBeneficiaria("kc-benef", conUsuario));

    entityManager().flush();
    entityManager().clear();

    Beneficiaria refrescadaConUsuario = entityManager().find(Beneficiaria.class, conUsuario.getId());
    Beneficiaria refrescadaSinUsuario = entityManager().find(Beneficiaria.class, sinUsuario.getId());

    assertTrue(repositorio.existeParaBeneficiaria(refrescadaConUsuario));
    assertFalse(repositorio.existeParaBeneficiaria(refrescadaSinUsuario));
  }

  private Donante donantePersistido(String nombre, String documento) {
    PersonaHumana persona = new PersonaHumana(nombre, "Test", 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    return donante;
  }

  private Beneficiaria beneficiariaPersistida(String cuit) {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, "Beneficiaria " + cuit, TipoOrganizacion.ONG, "Rubro"
    );
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }
}