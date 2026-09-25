package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositorioDonantesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repositorio = new RepositorioDonantes();

  @Test
  void unDonanteDeUnaPersonaHumanaSeEncuentraPorSuDocumento() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    repositorio.guardar(new Donante(ana));

    entityManager().flush();
    entityManager().clear();

    Optional<Donante> recuperado = repositorio.buscarPorDocumento("12345678");

    assertTrue(recuperado.isPresent());
    assertEquals("12345678", recuperado.get().getPersona().getDocumento());
  }

  @Test
  void unDonanteDeUnaPersonaJuridicaSeEncuentraPorSuCuit() {
    PersonaJuridica arcos = new PersonaJuridica(
        "30-12345678-1", "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repoPersonas.guardar(arcos);
    repositorio.guardar(new Donante(arcos));

    entityManager().flush();
    entityManager().clear();

    Optional<Donante> recuperado = repositorio.buscarPorDocumento("30123456781");

    assertTrue(recuperado.isPresent());
    assertEquals("30123456781", recuperado.get().getPersona().getDocumento());
  }

  @Test
  void unDocumentoInexistenteDevuelveVacio() {
    Optional<Donante> recuperado = repositorio.buscarPorDocumento("99999999");

    assertTrue(recuperado.isEmpty());
  }

  @Test
  void unDonanteQueInteractuoHoyNoRequiereAviso() {
    LocalDate hoy = LocalDate.now();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    repositorio.guardar(new Donante(ana));

    entityManager().flush();
    entityManager().clear();

    List<Donante> requieren = repositorio.queRequierenAvisoDeInactividad(hoy);

    assertTrue(requieren.isEmpty());
  }

  @Test
  void unDonanteInactivoQueNuncaFueAvisadoRequiereAviso() {
    LocalDate hoy = LocalDate.now();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    repositorio.guardar(new Donante(ana));

    entityManager().flush();
    entityManager().clear();

    List<Donante> requieren = repositorio.queRequierenAvisoDeInactividad(hoy.plusDays(1));

    assertEquals(1, requieren.size());
    assertEquals("12345678", requieren.get(0).getPersona().getDocumento());
  }

  @Test
  void unDonanteYaAvisadoNoVuelveARequerirAviso() {
    LocalDate hoy = LocalDate.now();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    Donante donante = new Donante(ana);
    donante.registrarAvisoDeInactividad();
    repositorio.guardar(donante);

    entityManager().flush();
    entityManager().clear();

    List<Donante> requieren = repositorio.queRequierenAvisoDeInactividad(hoy.plusDays(1));

    assertTrue(requieren.isEmpty());
  }

  @Test
  void unDonanteAvisadoQueDespuesVolvioAInteractuarRequiereAvisoDeNuevo() {
    LocalDate hoy = LocalDate.now();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    repoPersonas.guardar(ana);
    Donante donante = new Donante(ana);
    repositorio.guardar(donante);

    entityManager().flush();

    entityManager().createQuery(
        "update Donante d set d.fechaUltimoAvisoInactividad = :aviso, "
            + "d.ultimaInteraccion = :interaccion where d.id = :id")
        .setParameter("aviso", hoy.minusDays(10))
        .setParameter("interaccion", hoy.minusDays(5))
        .setParameter("id", donante.getId())
        .executeUpdate();
    entityManager().clear();

    List<Donante> requieren = repositorio.queRequierenAvisoDeInactividad(hoy);

    assertEquals(1, requieren.size());
    assertEquals("12345678", requieren.get(0).getPersona().getDocumento());
  }

  @Test
  void todosDevuelveLosDonantesGuardados() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    PersonaHumana luis = new PersonaHumana("Luis", "Garcia", 45, "87654321", Genero.MASCULINO);
    repoPersonas.guardar(ana);
    repoPersonas.guardar(luis);
    repositorio.guardar(new Donante(ana));
    repositorio.guardar(new Donante(luis));

    entityManager().flush();
    entityManager().clear();

    List<Donante> todos = repositorio.todos();

    assertEquals(2, todos.size());
  }
}
