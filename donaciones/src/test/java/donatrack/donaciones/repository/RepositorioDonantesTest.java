package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
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

import static org.junit.jupiter.api.Assertions.assertFalse;

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

    /*
    assertEquals(1, requieren.size());
    assertEquals("12345678", requieren.get(0).getPersona().getDocumento());
    */

    assertTrue(requieren.stream().anyMatch(d ->
    "12345678".equals(d.getPersona().getDocumento())));
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

    //assertTrue(requieren.isEmpty());

    assertFalse(requieren.stream().anyMatch(d ->
    "12345678".equals(d.getPersona().getDocumento())));
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

  // Otros tests del run comparten la base: cada test usa un apellido unico y busca por
  // el, asi el total y el orden no dependen de filas ajenas.

  @Test
  void laPaginaTraeSoloSusElementosOrdenadosPorNombreYElTotalEsElDelFiltro() {
    String apellido = DatosDeTest.textoUnico("Paginado");
    for (String nombre : List.of("Elena", "Carla", "Ana", "Dario", "Bruno")) {
      donanteHumano(nombre, apellido, DatosDeTest.documentoUnico());
    }
    entityManager().flush();
    entityManager().clear();

    assertEquals(5, repositorio.contar(apellido));
    assertEquals(List.of("Ana", "Bruno"), nombres(repositorio.pagina(apellido, 1, 2)));
    assertEquals(List.of("Carla", "Dario"), nombres(repositorio.pagina(apellido, 2, 2)));
    assertEquals(List.of("Elena"), nombres(repositorio.pagina(apellido, 3, 2)));
    assertTrue(repositorio.pagina(apellido, 4, 2).isEmpty());
  }

  @Test
  void laPaginaTraeLosContactosDeCadaDonante() {
    String apellido = DatosDeTest.textoUnico("ConContactos");
    String mail = DatosDeTest.emailUnico("ana");
    Donante ana = donanteHumano("Ana", apellido, DatosDeTest.documentoUnico());
    ana.getPersona().agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, mail));
    entityManager().flush();
    entityManager().clear();

    List<Donante> pagina = repositorio.pagina(apellido, 1, 20);

    assertEquals(1, pagina.size());
    assertEquals(mail, pagina.get(0).getPersona().getContactos().get(0).getValor());
  }

  @Test
  void laBusquedaNoDistingueMayusculasYCubreNombreRazonSocialDocumentoYMail() {
    String apellido = DatosDeTest.textoUnico("Buscado");
    String documento = DatosDeTest.documentoUnico();
    Donante humano = donanteHumano("Ana", apellido, documento);
    String mail = DatosDeTest.emailUnico("contacto-" + apellido);
    humano.getPersona().agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, mail));

    String razonSocial = DatosDeTest.textoUnico("Ferreteria Buscada");
    String cuit = DatosDeTest.cuitUnico();
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, razonSocial, TipoOrganizacion.EMPRESA, "Rubro");
    repoPersonas.guardar(juridica);
    repositorio.guardar(new Donante(juridica));

    entityManager().flush();
    entityManager().clear();

    assertEquals(List.of(documento), documentos(apellido.toUpperCase()));
    assertEquals(List.of(documento), documentos("ana " + apellido.toLowerCase()));
    assertEquals(List.of(documento), documentos(documento));
    assertEquals(List.of(documento), documentos(mail.toUpperCase()));
    assertEquals(List.of(cuit), documentos(razonSocial.toLowerCase()));
    assertEquals(List.of(cuit), documentos(cuit));
    String cuitConGuiones = cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-"
        + cuit.substring(10);
    assertEquals(List.of(cuit), documentos(cuitConGuiones));
    assertEquals(0, repositorio.contar(DatosDeTest.textoUnico("NoExiste")));
  }

  @Test
  void sinBusquedaElTotalCuentaATodosLosDonantes() {
    long antes = repositorio.contar(null);
    donanteHumano("Ana", DatosDeTest.textoUnico("Total"), DatosDeTest.documentoUnico());
    entityManager().flush();

    assertEquals(antes + 1, repositorio.contar(null));
    assertEquals(antes + 1, repositorio.contar(" "));
    assertTrue(repositorio.pagina(null, 1, 3).size() <= 3);
  }

  private Donante donanteHumano(String nombre, String apellido, String documento) {
    PersonaHumana persona = new PersonaHumana(nombre, apellido, 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repositorio.guardar(donante);
    return donante;
  }

  private List<String> documentos(String busqueda) {
    return repositorio.pagina(busqueda, 1, 20).stream()
        .map(d -> d.getPersona().getDocumento())
        .toList();
  }

  private static List<String> nombres(List<Donante> donantes) {
    return donantes.stream()
        .map(d -> ((PersonaHumana) d.getPersona()).getNombre())
        .toList();
  }
}
