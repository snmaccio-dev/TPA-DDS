package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositorioPersonasTest implements SimplePersistenceTest {

  private final RepositorioPersonas repositorio = RepositorioPersonas.getInstance();

  @Test
  void unaPersonaHumanaGuardadaSeRecuperaPorDocumento() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    ana.setDireccion("Av. Corrientes 1234, CABA");
    repositorio.guardar(ana);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> recuperada = repositorio.buscarPorDocumento("12345678");

    assertTrue(recuperada.isPresent());
    assertTrue(recuperada.get() instanceof PersonaHumana);
    assertEquals("Ana Perez", recuperada.get().getNombreDisplay());
    assertEquals("12345678", recuperada.get().getDocumento());
    assertEquals("Av. Corrientes 1234, CABA", recuperada.get().getDireccion());
  }

  @Test
  void unaPersonaJuridicaGuardadaSeRecuperaPorCuit() {
    PersonaJuridica arcos = new PersonaJuridica(
        "30-12345678-1", "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    Optional<PersonaJuridica> recuperada = repositorio.buscarJuridicaPorCuit("30123456781");

    assertTrue(recuperada.isPresent());
    assertEquals("Arcos S.A.", recuperada.get().getRazonSocial());
    assertEquals("30123456781", recuperada.get().getCuit());
  }

  @Test
  void buscarPorDocumentoDeHumanaNoDevuelveUnaJuridica() {
    PersonaJuridica arcos = new PersonaJuridica(
        "30-99999999-1", "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> resultado = repositorio.buscarPorDocumento("12345678");

    assertTrue(resultado.isEmpty());
  }

  @Test
  void buscarPorEmailEncuentraLaPersonaPorSuMedioContacto() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    repositorio.guardar(ana);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> recuperada = repositorio.buscarPorEmail("ana@mail.com");

    assertTrue(recuperada.isPresent());
    assertEquals("12345678", recuperada.get().getDocumento());
  }

  @Test
  void todosDevuelveTantoHumanasComoJuridicas() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    PersonaJuridica arcos = new PersonaJuridica(
        "30-12345678-1", "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(ana);
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    List<Persona> todos = repositorio.todos();

    assertEquals(2, todos.size());
    assertTrue(todos.stream().anyMatch(p -> p instanceof PersonaHumana));
    assertTrue(todos.stream().anyMatch(p -> p instanceof PersonaJuridica));
    assertFalse(todos.isEmpty());
    assertNotNull(todos.get(0));
  }
}
