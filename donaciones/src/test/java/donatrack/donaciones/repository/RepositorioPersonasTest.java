package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
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
  void eliminarPersonaHumanaConContactoPredeterminadoBorraLaPersonaYSusMedios() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "66666666", Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "+54 11 0000-0000"));
    repositorio.guardar(ana);
    entityManager().flush();
    assertNotNull(ana.getContactoPredeterminado(),
        "El primer medio agregado queda como predeterminado.");
    Long personaId = ana.getId();
    List<Long> mediosIds = ana.getContactos().stream()
        .map(MedioContacto::getId)
        .toList();

    entityManager().getTransaction().begin();
    repositorio.eliminar(ana);
    entityManager().getTransaction().commit();
    entityManager().clear();

    assertTrue(repositorio.buscarPorDocumento("66666666").isEmpty());
    for (Long id : mediosIds) {
      assertNull(entityManager().find(MedioContacto.class, id),
          "Los medios de contacto tienen que haber caido por cascade.");
    }
    assertNull(entityManager().find(PersonaHumana.class, personaId));
  }

  @Test
  void eliminarPersonaJuridicaConRepresentantesYContactoPredeterminadoBorraTodo() {
    PersonaJuridica arcos = new PersonaJuridica(
        "30-55555555-5", "Arcos", TipoOrganizacion.EMPRESA, "Rubro");
    arcos.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "arcos@mail.com"));
    arcos.agregarRepresentante(new Representante("Rep", "Uno", "rep1@mail.com"));
    arcos.agregarRepresentante(new Representante("Rep", "Dos", "rep2@mail.com"));
    repositorio.guardar(arcos);
    entityManager().flush();
    assertNotNull(arcos.getContactoPredeterminado());

    entityManager().getTransaction().begin();
    repositorio.eliminar(arcos);
    entityManager().getTransaction().commit();
    entityManager().clear();

    assertTrue(repositorio.buscarJuridicaPorCuit("30555555555").isEmpty());
  }

  @Test
  void eliminarPersonaSinContactoPredeterminadoTambienFunciona() {
    PersonaHumana sinContacto = new PersonaHumana("X", "Y", 30, "77777777", Genero.FEMENINO);
    repositorio.guardar(sinContacto);
    entityManager().flush();
    assertNull(sinContacto.getContactoPredeterminado());

    entityManager().getTransaction().begin();
    repositorio.eliminar(sinContacto);
    entityManager().getTransaction().commit();
    entityManager().clear();

    assertTrue(repositorio.buscarPorDocumento("77777777").isEmpty());
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
