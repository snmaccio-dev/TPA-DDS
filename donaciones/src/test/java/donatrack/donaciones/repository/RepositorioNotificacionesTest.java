package donatrack.donaciones.repository;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioNotificacionesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioNotificaciones repositorio = new RepositorioNotificaciones();

  @Test
  void donanteYBeneficiariaDeLaMismaPersonaJuridicaTienenListasSeparadas() {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-00000000-1", "Dual", TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Donante donante = new Donante(juridica);
    repoDonantes.guardar(donante);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);

    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "soy del donante", null));
    repositorio.guardar(Notificacion.paraBeneficiaria(
        beneficiaria, TipoNotificacion.DONACION_ASIGNADA, "soy de la benef", null));

    entityManager().flush();
    entityManager().clear();

    Donante refrescadoD = entityManager().find(Donante.class, donante.getId());
    Beneficiaria refrescadaB = entityManager().find(Beneficiaria.class, beneficiaria.getId());

    List<Notificacion> delDonante = repositorio.deDonante(refrescadoD, false, 0, 10);
    List<Notificacion> deLaBenef = repositorio.deBeneficiaria(refrescadaB, false, 0, 10);

    assertEquals(1, delDonante.size());
    assertEquals("soy del donante", delDonante.get(0).getMensaje());
    assertEquals(1, deLaBenef.size());
    assertEquals("soy de la benef", deLaBenef.get(0).getMensaje());
  }

  @Test
  void marcarLeidasDeDonanteAfectaMasDeCienYDevuelveLaCantidad() {
    Donante donante = donantePersistido("Ana", "11111111");

    for (int i = 0; i < 150; i++) {
      repositorio.guardar(Notificacion.paraDonante(
          donante, TipoNotificacion.CAMBIO_ESTADO, "msj " + i, null));
    }
    entityManager().flush();
    entityManager().clear();

    Donante refrescado = entityManager().find(Donante.class, donante.getId());

    entityManager().getTransaction().begin();
    int afectadas = repositorio.marcarLeidasDeDonante(refrescado, LocalDateTime.now());
    entityManager().getTransaction().commit();

    assertEquals(150, afectadas);
    assertEquals(0, repositorio.cantidadNoLeidasDeDonante(refrescado));
  }

  @Test
  void marcarLeidasDeAdministradoresAfectaSoloLasDeAdmin() {
    Donante donante = donantePersistido("Luis", "22222222");

    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "personal", null));
    for (int i = 0; i < 3; i++) {
      repositorio.guardar(Notificacion.paraAdministradores(
          TipoNotificacion.ENTREGA_FALLIDA, "admin " + i, null));
    }
    entityManager().flush();
    entityManager().clear();

    entityManager().getTransaction().begin();
    int afectadas = repositorio.marcarLeidasDeAdministradores(LocalDateTime.now());
    entityManager().getTransaction().commit();

    Donante refrescado = entityManager().find(Donante.class, donante.getId());

    assertEquals(3, afectadas);
    assertEquals(1, repositorio.cantidadNoLeidasDeDonante(refrescado),
        "La personal del donante no se tiene que haber marcado.");
  }

  @Test
  void eliminarDeDonacionBorraSoloLasDeEsaDonacion() {
    Donante donante = donantePersistido("Delete", "33333333");
    Donacion donacion = donacionDe(donante);
    Donacion otra = donacionDe(donante);

    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "de la donacion", donacion));
    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "de la otra", otra));
    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.AUSENCIA_PLATAFORMA, "sin donacion", null));
    entityManager().flush();
    entityManager().clear();

    Donacion refrescada = entityManager().find(Donacion.class, donacion.getId());

    entityManager().getTransaction().begin();
    int borradas = repositorio.eliminarDeDonacion(refrescada);
    entityManager().getTransaction().commit();
    entityManager().clear();

    Donante refrescadoD = entityManager().find(Donante.class, donante.getId());

    assertEquals(1, borradas);
    List<Notificacion> remanentes = repositorio.deDonante(refrescadoD, false, 0, 10);
    assertEquals(2, remanentes.size());
    assertTrue(remanentes.stream().noneMatch(n -> "de la donacion".equals(n.getMensaje())));
  }

  @Test
  void eliminarDeDonanteYDeBeneficiariaBorranSoloLoDeCadaPerfil() {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-99999999-1", "Dual2", TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Donante donante = new Donante(juridica);
    repoDonantes.guardar(donante);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);

    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "D1", null));
    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "D2", null));
    repositorio.guardar(Notificacion.paraBeneficiaria(
        beneficiaria, TipoNotificacion.DONACION_ASIGNADA, "B1", null));

    entityManager().flush();
    entityManager().clear();

    Donante refD = entityManager().find(Donante.class, donante.getId());
    Beneficiaria refB = entityManager().find(Beneficiaria.class, beneficiaria.getId());

    entityManager().getTransaction().begin();
    int borradasD = repositorio.eliminarDeDonante(refD);
    entityManager().getTransaction().commit();

    assertEquals(2, borradasD);
    assertEquals(0, repositorio.deDonante(refD, false, 0, 10).size());
    assertEquals(1, repositorio.deBeneficiaria(refB, false, 0, 10).size());

    entityManager().getTransaction().begin();
    int borradasB = repositorio.eliminarDeBeneficiaria(refB);
    entityManager().getTransaction().commit();

    assertEquals(1, borradasB);
    assertEquals(0, repositorio.deBeneficiaria(refB, false, 0, 10).size());
  }

  @Test
  void deAdministradoresDevuelveSoloLasDeAdmin() {
    Donante donante = donantePersistido("Ana", "44444444");
    repositorio.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "personal", null));
    repositorio.guardar(Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "para admins", null));
    entityManager().flush();
    entityManager().clear();

    List<Notificacion> deAdmins = repositorio.deAdministradores(false, 0, 10);
    assertEquals(1, deAdmins.size());
    assertEquals("para admins", deAdmins.get(0).getMensaje());
  }

  @Test
  void todasFiltraPorEstadoEnvioYTipo() {
    Donante donante = donantePersistido("Ana", "55555555");

    Notificacion enviada = Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "enviada", null);
    enviada.marcarEnviada(
        donatrack.donaciones.domain.contacto.TipoContacto.EMAIL, "ana@mail.com");
    Notificacion fallida = Notificacion.paraDonante(
        donante, TipoNotificacion.ENTREGA_FALLIDA, "fallida", null);
    fallida.marcarFallida("sin contacto");
    repositorio.guardar(enviada);
    repositorio.guardar(fallida);

    entityManager().flush();
    entityManager().clear();

    List<Notificacion> soloFallidas = repositorio.todas(EstadoEnvio.FALLIDA, null, 0, 10);
    assertEquals(1, soloFallidas.size());
    assertEquals("fallida", soloFallidas.get(0).getMensaje());

    List<Notificacion> soloEntregasFallidas =
        repositorio.todas(null, TipoNotificacion.ENTREGA_FALLIDA, 0, 10);
    assertEquals(1, soloEntregasFallidas.size());
  }

  // === Helpers ===

  private Donante donantePersistido(String nombre, String documento) {
    PersonaHumana persona = new PersonaHumana(nombre, "Test", 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    return donante;
  }

  private Donacion donacionDe(Donante donante) {
    Categoria mobiliario = new Categoria("Mobiliario");
    entityManager().getTransaction().begin();
    entityManager().persist(mobiliario);
    Subcategoria sillas = new Subcategoria("Sillas-" + System.nanoTime(), mobiliario, Unidades.UNIDADES);
    entityManager().persist(sillas);
    Bien bien = new Bien("Silla", sillas, 1, CondicionBien.USADO);
    Donacion donacion = new Donacion(List.of(bien), donante, "demo");
    entityManager().persist(donacion);
    entityManager().getTransaction().commit();
    return donacion;
  }
}
