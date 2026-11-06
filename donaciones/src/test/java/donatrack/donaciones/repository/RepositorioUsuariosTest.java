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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioUsuariosTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repositorio = new RepositorioUsuarios();

  @Test
  void guardarYBuscarPorKeycloakId() {
    Donante donante = donantePersistido("Ana", "11111111");

    repositorio.guardar(Usuario.deDonante("kc-111", donante));

    entityManager().flush();
    entityManager().clear();

    Optional<Usuario> recuperado = repositorio.buscarPorKeycloakId("kc-111");

    assertTrue(recuperado.isPresent());
    assertEquals("kc-111", recuperado.get().getKeycloakId());
    assertEquals(donante.getId(), recuperado.get().getDonante().getId());
  }

  @Test
  void unSegundoUsuarioParaElMismoDonanteFallaAlFlush() {
    Donante donante = donantePersistido("Luis", "22222222");

    repositorio.guardar(Usuario.deDonante("kc-A", donante));
    repositorio.guardar(Usuario.deDonante("kc-B", donante));

    assertThrows(RuntimeException.class, () -> entityManager().flush(),
        "La columna unique de donante_id debe rechazar el segundo usuario.");
  }

  @Test
  void deDonanteEncuentraElVinculo() {
    Donante donante = donantePersistido("Maria", "33333333");
    repositorio.guardar(Usuario.deDonante("kc-maria", donante));

    entityManager().flush();
    entityManager().clear();

    Donante refrescado = entityManager().find(Donante.class, donante.getId());
    Optional<Usuario> usuario = repositorio.deDonante(refrescado);

    assertTrue(usuario.isPresent());
    assertEquals("kc-maria", usuario.get().getKeycloakId());
  }

  @Test
  void deBeneficiariaEncuentraElVinculo() {
    Beneficiaria beneficiaria = beneficiariaPersistida("30-11111111-1");
    repositorio.guardar(Usuario.deBeneficiaria("kc-benef", beneficiaria));

    entityManager().flush();
    entityManager().clear();

    Beneficiaria refrescada = entityManager().find(Beneficiaria.class, beneficiaria.getId());
    Optional<Usuario> usuario = repositorio.deBeneficiaria(refrescada);

    assertTrue(usuario.isPresent());
    assertEquals("kc-benef", usuario.get().getKeycloakId());
  }

  @Test
  void eliminarQuitaElVinculo() {
    Donante donante = donantePersistido("Delete", "44444444");
    repositorio.guardar(Usuario.deDonante("kc-del", donante));

    entityManager().flush();
    entityManager().clear();

    Usuario usuario = repositorio.buscarPorKeycloakId("kc-del").orElseThrow();
    entityManager().getTransaction().begin();
    repositorio.eliminar(usuario);
    entityManager().getTransaction().commit();
    entityManager().clear();

    assertFalse(repositorio.buscarPorKeycloakId("kc-del").isPresent());
  }

  @Test
  void existeParaDonanteDistingueDonantes() {
    Donante conUsuario = donantePersistido("Con", "55555555");
    Donante sinUsuario = donantePersistido("Sin", "66666666");
    repositorio.guardar(Usuario.deDonante("kc-con", conUsuario));

    entityManager().flush();
    entityManager().clear();

    Donante refrescadoCon = entityManager().find(Donante.class, conUsuario.getId());
    Donante refrescadoSin = entityManager().find(Donante.class, sinUsuario.getId());

    assertTrue(repositorio.existeParaDonante(refrescadoCon));
    assertFalse(repositorio.existeParaDonante(refrescadoSin));
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
        cuit, "Beneficiaria " + cuit, TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }
}
