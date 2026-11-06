package donatrack.donaciones.repository;

import donatrack.donaciones.DatosDeTest;
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
    Donante donante = donantePersistido("Ana");
    String kcId = DatosDeTest.keycloakIdUnico();

    repositorio.guardar(Usuario.deDonante(kcId, donante));

    entityManager().flush();
    entityManager().clear();

    Optional<Usuario> recuperado = repositorio.buscarPorKeycloakId(kcId);

    assertTrue(recuperado.isPresent());
    assertEquals(kcId, recuperado.get().getKeycloakId());
    assertEquals(donante.getId(), recuperado.get().getDonante().getId());
  }

  @Test
  void unSegundoUsuarioParaElMismoDonanteFallaAlFlush() {
    Donante donante = donantePersistido("Luis");

    repositorio.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));
    repositorio.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));

    assertThrows(RuntimeException.class, () -> entityManager().flush(),
        "La columna unique de donante_id debe rechazar el segundo usuario.");
  }

  @Test
  void deDonanteEncuentraElVinculo() {
    Donante donante = donantePersistido("Maria");
    String kcId = DatosDeTest.keycloakIdUnico();
    repositorio.guardar(Usuario.deDonante(kcId, donante));

    entityManager().flush();
    entityManager().clear();

    Donante refrescado = entityManager().find(Donante.class, donante.getId());
    Optional<Usuario> usuario = repositorio.deDonante(refrescado);

    assertTrue(usuario.isPresent());
    assertEquals(kcId, usuario.get().getKeycloakId());
  }

  @Test
  void deBeneficiariaEncuentraElVinculo() {
    Beneficiaria beneficiaria = beneficiariaPersistida();
    String kcId = DatosDeTest.keycloakIdUnico();
    repositorio.guardar(Usuario.deBeneficiaria(kcId, beneficiaria));

    entityManager().flush();
    entityManager().clear();

    Beneficiaria refrescada = entityManager().find(Beneficiaria.class, beneficiaria.getId());
    Optional<Usuario> usuario = repositorio.deBeneficiaria(refrescada);

    assertTrue(usuario.isPresent());
    assertEquals(kcId, usuario.get().getKeycloakId());
  }

  @Test
  void eliminarQuitaElVinculo() {
    Donante donante = donantePersistido("Delete");
    String kcId = DatosDeTest.keycloakIdUnico();
    repositorio.guardar(Usuario.deDonante(kcId, donante));

    entityManager().flush();
    entityManager().clear();

    Usuario usuario = repositorio.buscarPorKeycloakId(kcId).orElseThrow();
    // La transaccion la abre SimplePersistenceTest. flush + clear para
    // que el delete se refleje al releer.
    repositorio.eliminar(usuario);
    entityManager().flush();
    entityManager().clear();

    assertFalse(repositorio.buscarPorKeycloakId(kcId).isPresent());
  }

  @Test
  void existeParaDonanteDistingueDonantes() {
    Donante conUsuario = donantePersistido("Con");
    Donante sinUsuario = donantePersistido("Sin");
    repositorio.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), conUsuario));

    entityManager().flush();
    entityManager().clear();

    Donante refrescadoCon = entityManager().find(Donante.class, conUsuario.getId());
    Donante refrescadoSin = entityManager().find(Donante.class, sinUsuario.getId());

    assertTrue(repositorio.existeParaDonante(refrescadoCon));
    assertFalse(repositorio.existeParaDonante(refrescadoSin));
  }

  private Donante donantePersistido(String nombre) {
    PersonaHumana persona = new PersonaHumana(
        nombre, "Test", 30, DatosDeTest.documentoUnico(), Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    return donante;
  }

  private Beneficiaria beneficiariaPersistida() {
    String cuit = DatosDeTest.cuitUnico();
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, "Beneficiaria " + cuit, TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }

}
