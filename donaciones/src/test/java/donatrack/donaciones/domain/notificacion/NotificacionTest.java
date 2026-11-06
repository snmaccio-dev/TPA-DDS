package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
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
  public void paraDonanteGuardaReferenciaYArrancaEnNoAplica() {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    Notificacion n = Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertSame(donante, n.getDonante());
    assertNull(n.getBeneficiaria());
    assertFalse(n.esParaAdministradores());
    assertEquals(EstadoEnvio.NO_APLICA, n.getEstadoEnvio());
    assertNotNull(n.getFechaHoraCreacion());
    assertNull(n.getFechaHoraLectura());
  }

  @Test
  public void paraBeneficiariaGuardaReferencia() {
    Beneficiaria beneficiaria = new Beneficiaria(
        new PersonaJuridica("30-1-1", "Comedor X", TipoOrganizacion.ONG, "Comedor"));
    Notificacion n = Notificacion.paraBeneficiaria(
        beneficiaria, TipoNotificacion.DONACION_ASIGNADA, "asignada", null);

    assertSame(beneficiaria, n.getBeneficiaria());
    assertNull(n.getDonante());
    assertFalse(n.esParaAdministradores());
  }

  @Test
  public void paraAdministradoresNoTienePerfil() {
    Notificacion n = Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "ups", null);

    assertNull(n.getDonante());
    assertNull(n.getBeneficiaria());
    assertTrue(n.esParaAdministradores());
  }

  @Test
  public void fabricasValidanPerfilNull() {
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraDonante(null, TipoNotificacion.CAMBIO_ESTADO, "x", null));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraBeneficiaria(null, TipoNotificacion.CAMBIO_ESTADO, "x", null));
  }

  @Test
  public void fabricasValidanTipoYMensaje() {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraDonante(donante, null, "hola", null));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraDonante(donante, TipoNotificacion.CAMBIO_ESTADO, "", null));
    assertThrows(DatosInvalidosException.class, () ->
        Notificacion.paraDonante(donante, TipoNotificacion.CAMBIO_ESTADO, null, null));
  }

  @Test
  public void marcarEnviadaGuardaCanalYDestino() {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    Notificacion n = Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    n.marcarEnviada(TipoContacto.EMAIL, "ana@mail.com");

    assertEquals(EstadoEnvio.ENVIADA, n.getEstadoEnvio());
    assertEquals(TipoContacto.EMAIL, n.getCanal());
    assertEquals("ana@mail.com", n.getDestino());
    assertNull(n.getDetalleError());
  }

  @Test
  public void marcarFallidaGuardaDetalle() {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    Notificacion n = Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    n.marcarFallida("sin contacto");

    assertEquals(EstadoEnvio.FALLIDA, n.getEstadoEnvio());
    assertEquals("sin contacto", n.getDetalleError());
  }

  @Test
  public void marcarLeidaEsIdempotente() throws InterruptedException {
    Donante donante = new Donante(
        new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO));
    Notificacion n = Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertFalse(n.estaLeida());
    n.marcarLeida();
    LocalDateTime primera = n.getFechaHoraLectura();
    assertTrue(n.estaLeida());
    Thread.sleep(2);
    n.marcarLeida();
    assertSame(primera, n.getFechaHoraLectura());
  }
}
