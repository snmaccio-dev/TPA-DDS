package donatrack.donaciones.repository;

import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioNotificacionesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioNotificaciones repositorio = new RepositorioNotificaciones();

  @Test
  void deDestinatarioOrdenaDescendenteYPagina() throws InterruptedException {
    PersonaHumana ana = personaPersistida("Ana", "11111111");

    for (int i = 0; i < 5; i++) {
      Notificacion n = Notificacion.paraPersona(
          ana, TipoNotificacion.CAMBIO_ESTADO, "mensaje " + i, null);
      repositorio.guardar(n);
      // Separamos las fechas para poder verificar el orden.
      Thread.sleep(2);
    }
    entityManager().flush();
    entityManager().clear();

    PersonaHumana refrescada = entityManager().find(PersonaHumana.class, ana.getId());

    List<Notificacion> primeraPagina = repositorio.deDestinatario(refrescada, false, 0, 2);
    List<Notificacion> segundaPagina = repositorio.deDestinatario(refrescada, false, 1, 2);

    assertEquals(2, primeraPagina.size());
    assertEquals(2, segundaPagina.size());
    assertTrue(primeraPagina.get(0).getFechaHoraCreacion()
            .isAfter(primeraPagina.get(1).getFechaHoraCreacion()),
        "Las notificaciones vienen ordenadas por fecha descendente.");
    assertTrue(segundaPagina.get(0).getFechaHoraCreacion()
            .isBefore(primeraPagina.get(1).getFechaHoraCreacion()),
        "La segunda pagina trae mas viejas que la primera.");
  }

  @Test
  void deDestinatarioConSoloNoLeidasFiltraLasYaLeidas() {
    PersonaHumana ana = personaPersistida("Ana", "22222222");

    Notificacion leida = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "ya leida", null);
    leida.marcarLeida();
    Notificacion noLeida = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "sin leer", null);
    repositorio.guardar(leida);
    repositorio.guardar(noLeida);

    entityManager().flush();
    entityManager().clear();

    PersonaHumana refrescada = entityManager().find(PersonaHumana.class, ana.getId());
    List<Notificacion> soloNoLeidas = repositorio.deDestinatario(refrescada, true, 0, 10);

    assertEquals(1, soloNoLeidas.size());
    assertEquals("sin leer", soloNoLeidas.get(0).getMensaje());
  }

  @Test
  void deAdministradoresDevuelveSoloLasDeAdmin() {
    PersonaHumana ana = personaPersistida("Ana", "33333333");
    repositorio.guardar(Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "personal", null));
    repositorio.guardar(Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "para admins", null));

    entityManager().flush();
    entityManager().clear();

    List<Notificacion> deAdmins = repositorio.deAdministradores(false, 0, 10);
    assertEquals(1, deAdmins.size());
    assertEquals("para admins", deAdmins.get(0).getMensaje());
  }

  @Test
  void cantidadNoLeidas() {
    PersonaHumana ana = personaPersistida("Ana", "44444444");
    Notificacion leida = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "ya leida", null);
    leida.marcarLeida();
    repositorio.guardar(leida);
    repositorio.guardar(Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "sin leer 1", null));
    repositorio.guardar(Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "sin leer 2", null));
    repositorio.guardar(Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "admin sin leer", null));

    entityManager().flush();
    entityManager().clear();

    PersonaHumana refrescada = entityManager().find(PersonaHumana.class, ana.getId());
    assertEquals(2, repositorio.cantidadNoLeidas(refrescada));
    assertEquals(1, repositorio.cantidadNoLeidasAdministradores());
  }

  @Test
  void todasFiltraPorEstadoEnvioYTipo() {
    PersonaHumana ana = personaPersistida("Ana", "55555555");

    Notificacion enviada = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "enviada", null);
    enviada.marcarEnviada(donatrack.donaciones.domain.contacto.TipoContacto.EMAIL, "ana@mail.com");
    Notificacion fallida = Notificacion.paraPersona(
        ana, TipoNotificacion.ENTREGA_FALLIDA, "fallida", null);
    fallida.marcarFallida("sin contacto");
    repositorio.guardar(enviada);
    repositorio.guardar(fallida);

    entityManager().flush();
    entityManager().clear();

    List<Notificacion> soloFallidas = repositorio.todas(EstadoEnvio.FALLIDA, null, 0, 10);
    assertEquals(1, soloFallidas.size());
    assertEquals("fallida", soloFallidas.get(0).getMensaje());

    List<Notificacion> soloEntregasFallidas = repositorio.todas(null, TipoNotificacion.ENTREGA_FALLIDA, 0, 10);
    assertEquals(1, soloEntregasFallidas.size());
    assertEquals("fallida", soloEntregasFallidas.get(0).getMensaje());
  }

  private PersonaHumana personaPersistida(String nombre, String documento) {
    PersonaHumana persona = new PersonaHumana(nombre, "Test", 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    return persona;
  }
}
