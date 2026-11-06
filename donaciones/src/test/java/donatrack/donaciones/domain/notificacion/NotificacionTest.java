package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificacionTest {

  @Test
  public void paraPersonaDejaLaReferenciaAlDestinatarioYArrancaEnNoAplica() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    Notificacion n = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertSame(ana, n.getDestinatario());
    assertFalse(n.esParaAdministradores());
    assertEquals(EstadoEnvio.NO_APLICA, n.getEstadoEnvio());
    assertNotNull(n.getFechaHoraCreacion());
    assertNull(n.getFechaHoraLectura());
  }

  @Test
  public void paraAdministradoresNoTieneDestinatarioPersonal() {
    Notificacion n = Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "ups", null);

    assertNull(n.getDestinatario());
    assertTrue(n.esParaAdministradores());
  }

  @Test
  public void paraPersonaExigeDestinatario() {
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraPersona(null, TipoNotificacion.CAMBIO_ESTADO, "hola", null));
  }

  @Test
  public void paraPersonaExigeTipoYMensaje() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraPersona(ana, null, "hola", null));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraPersona(ana, TipoNotificacion.CAMBIO_ESTADO, "", null));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraPersona(ana, TipoNotificacion.CAMBIO_ESTADO, null, null));
  }

  @Test
  public void marcarEnviadaGuardaCanalYDestino() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    Notificacion n = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    n.marcarEnviada(TipoContacto.EMAIL, "ana@mail.com");

    assertEquals(EstadoEnvio.ENVIADA, n.getEstadoEnvio());
    assertEquals(TipoContacto.EMAIL, n.getCanal());
    assertEquals("ana@mail.com", n.getDestino());
    assertNull(n.getDetalleError());
  }

  @Test
  public void marcarFallidaGuardaDetalle() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    Notificacion n = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    n.marcarFallida("sin medio de contacto");

    assertEquals(EstadoEnvio.FALLIDA, n.getEstadoEnvio());
    assertEquals("sin medio de contacto", n.getDetalleError());
  }

  @Test
  public void marcarLeidaEsIdempotenteYNoPisaLaFechaOriginal() throws InterruptedException {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    Notificacion n = Notificacion.paraPersona(
        ana, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertFalse(n.estaLeida());
    n.marcarLeida();
    LocalDateTime primeraLectura = n.getFechaHoraLectura();
    assertTrue(n.estaLeida());
    assertNotNull(primeraLectura);

    Thread.sleep(2);
    n.marcarLeida();
    assertSame(primeraLectura, n.getFechaHoraLectura(),
        "marcarLeida tiene que ser idempotente: no debe pisar la fecha.");
  }
}
