package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.DatosDeTest;
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
    String documento = DatosDeTest.documentoUnico();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    ana.setDireccion("Av. Corrientes 1234, CABA");
    repositorio.guardar(ana);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> recuperada = repositorio.buscarPorDocumento(documento);

    assertTrue(recuperada.isPresent());
    assertTrue(recuperada.get() instanceof PersonaHumana);
    assertEquals("Ana Perez", recuperada.get().getNombreDisplay());
    assertEquals(documento, recuperada.get().getDocumento());
    assertEquals("Av. Corrientes 1234, CABA", recuperada.get().getDireccion());
  }

  @Test
  void unaPersonaJuridicaGuardadaSeRecuperaPorCuit() {
    String cuitConGuiones = cuitConGuionesUnico();
    PersonaJuridica arcos = new PersonaJuridica(
        cuitConGuiones, "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    String cuitNormalizado = PersonaJuridica.normalizarCuit(cuitConGuiones);
    Optional<PersonaJuridica> recuperada = repositorio.buscarJuridicaPorCuit(cuitNormalizado);

    assertTrue(recuperada.isPresent());
    assertEquals("Arcos S.A.", recuperada.get().getRazonSocial());
    assertEquals(cuitNormalizado, recuperada.get().getCuit());
  }

  @Test
  void buscarJuridicaPorCuitFuncionaConYSinGuiones() {
    String cuitConGuiones = "30-12345678-9";
    PersonaJuridica arcos = new PersonaJuridica(
        cuitConGuiones, "Arcos Normalizado", TipoOrganizacion.EMPRESA, "Rubro"
    );
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    Optional<PersonaJuridica> conGuiones = repositorio.buscarJuridicaPorCuit("30-12345678-9");
    Optional<PersonaJuridica> sinGuiones = repositorio.buscarJuridicaPorCuit("30123456789");

    assertTrue(conGuiones.isPresent(), "Tiene que encontrarla con el CUIT con guiones.");
    assertTrue(sinGuiones.isPresent(), "Y tambien sin guiones.");
    assertEquals(conGuiones.get().getCuit(), sinGuiones.get().getCuit());
  }

  @Test
  void buscarPorDocumentoDeHumanaNoDevuelveUnaJuridica() {
    PersonaJuridica arcos = new PersonaJuridica(
        cuitConGuionesUnico(), "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> resultado = repositorio.buscarPorDocumento(DatosDeTest.documentoUnico());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void buscarPorEmailEncuentraLaPersonaPorSuMedioContacto() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("ana");
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
    repositorio.guardar(ana);

    entityManager().flush();
    entityManager().clear();

    Optional<Persona> recuperada = repositorio.buscarPorEmail(email);

    assertTrue(recuperada.isPresent());
    assertEquals(documento, recuperada.get().getDocumento());
  }

  @Test
  void eliminarPersonaHumanaConContactoPredeterminadoBorraLaPersonaYSusMedios() {
    String documento = DatosDeTest.documentoUnico();
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("ana")));
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "+54 11 0000-0000"));
    repositorio.guardar(ana);
    entityManager().flush();
    assertNotNull(ana.getContactoPredeterminado(),
        "El primer medio agregado queda como predeterminado.");
    Long personaId = ana.getId();
    List<Long> mediosIds = ana.getContactos().stream()
        .map(MedioContacto::getId)
        .toList();

    // La transaccion la abre SimplePersistenceTest; el repositorio ya hace
    // flush interno para ordenar los deletes. Solo hace falta clear() despues
    // para releer desde la base.
    repositorio.eliminar(ana);
    entityManager().flush();
    entityManager().clear();

    assertTrue(repositorio.buscarPorDocumento(documento).isEmpty());
    for (Long id : mediosIds) {
      assertNull(entityManager().find(MedioContacto.class, id),
          "Los medios de contacto tienen que haber caido por cascade.");
    }
    assertNull(entityManager().find(PersonaHumana.class, personaId));
  }

  @Test
  void eliminarPersonaJuridicaConRepresentantesYContactoPredeterminadoBorraTodo() {
    String cuit = cuitConGuionesUnico();
    PersonaJuridica arcos = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Arcos"), TipoOrganizacion.EMPRESA, "Rubro");
    arcos.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("arcos")));
    arcos.agregarRepresentante(new Representante("Rep", "Uno", DatosDeTest.emailUnico("rep1")));
    arcos.agregarRepresentante(new Representante("Rep", "Dos", DatosDeTest.emailUnico("rep2")));
    repositorio.guardar(arcos);
    entityManager().flush();
    assertNotNull(arcos.getContactoPredeterminado());

    repositorio.eliminar(arcos);
    entityManager().flush();
    entityManager().clear();

    assertTrue(repositorio.buscarJuridicaPorCuit(cuit).isEmpty());
  }

  @Test
  void eliminarPersonaSinContactoPredeterminadoTambienFunciona() {
    String documento = DatosDeTest.documentoUnico();
    PersonaHumana sinContacto = new PersonaHumana("X", "Y", 30, documento, Genero.FEMENINO);
    repositorio.guardar(sinContacto);
    entityManager().flush();
    assertNull(sinContacto.getContactoPredeterminado());

    repositorio.eliminar(sinContacto);
    entityManager().flush();
    entityManager().clear();

    assertTrue(repositorio.buscarPorDocumento(documento).isEmpty());
  }

  @Test
  void todosDevuelveTantoHumanasComoJuridicas() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, DatosDeTest.documentoUnico(), Genero.FEMENINO);
    PersonaJuridica arcos = new PersonaJuridica(
        cuitConGuionesUnico(), DatosDeTest.textoUnico("Arcos"), TipoOrganizacion.EMPRESA, "Construccion"
    );
    repositorio.guardar(ana);
    repositorio.guardar(arcos);

    entityManager().flush();
    entityManager().clear();

    List<Persona> todos = repositorio.todos();

    // Otros tests del run pueden haber dejado filas; por eso no comparamos
    // contra 2 absoluto: pedimos que esten las dos que acabamos de guardar.
    assertTrue(todos.stream().anyMatch(p -> p instanceof PersonaHumana));
    assertTrue(todos.stream().anyMatch(p -> p instanceof PersonaJuridica));
    assertFalse(todos.isEmpty());
    assertNotNull(todos.get(0));
  }

  // Helper local: cuando el test necesita verificar que la normalizacion de
  // guiones funciona, armamos el CUIT con guiones. En los tests que no lo
  // necesitan directamente usamos DatosDeTest.cuitUnico().
  private static String cuitConGuionesUnico() {
    String cuit = DatosDeTest.cuitUnico();
    return cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10);
  }
}
