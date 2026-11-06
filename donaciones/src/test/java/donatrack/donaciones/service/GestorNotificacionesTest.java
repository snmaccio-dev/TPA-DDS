package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.Notificador;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorNotificacionesTest {

  @Test
  void unEnvioExitosoQuedaEnviadaConCanalYDestino() {
    RepositorioEnMemoria repo = new RepositorioEnMemoria();
    NotificadorFalso email = new NotificadorFalso(TipoContacto.EMAIL, false);
    GestorNotificaciones gestor = new GestorNotificaciones(List.of(email), repo);

    Donante donante = donanteAna();
    Notificacion n = gestor.notificar(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertEquals(EstadoEnvio.ENVIADA, n.getEstadoEnvio());
    assertEquals(TipoContacto.EMAIL, n.getCanal());
    assertEquals("ana@mail.com", n.getDestino());
    assertSame(donante, n.getDonante());
    assertEquals(1, email.recibidos.size());
    assertEquals(1, repo.guardadas.size());
  }

  @Test
  void siElNotificadorFallaQuedaFallidaYNoPropaga() {
    RepositorioEnMemoria repo = new RepositorioEnMemoria();
    NotificadorFalso email = new NotificadorFalso(TipoContacto.EMAIL, true);
    GestorNotificaciones gestor = new GestorNotificaciones(List.of(email), repo);

    Notificacion n = gestor.notificar(
        donanteAna(), TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertEquals(EstadoEnvio.FALLIDA, n.getEstadoEnvio());
    assertTrue(n.getDetalleError().contains("SMTP caido"));
    assertEquals(1, repo.guardadas.size());
  }

  @Test
  void unDonanteSinContactoQuedaFallida() {
    RepositorioEnMemoria repo = new RepositorioEnMemoria();
    GestorNotificaciones gestor = new GestorNotificaciones(
        List.of(new NotificadorFalso(TipoContacto.EMAIL, false)), repo);

    PersonaHumana sinContacto = new PersonaHumana("X", "Y", 30, "99999999", Genero.FEMENINO);
    Donante donante = new Donante(sinContacto);

    Notificacion n = gestor.notificar(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertEquals(EstadoEnvio.FALLIDA, n.getEstadoEnvio());
    assertTrue(n.getDetalleError().toLowerCase().contains("medio de contacto"));
    assertEquals(1, repo.guardadas.size());
  }

  @Test
  void notificarAdministradoresQuedaEnNoAplicaYSePersiste() {
    RepositorioEnMemoria repo = new RepositorioEnMemoria();
    GestorNotificaciones gestor = new GestorNotificaciones(List.of(), repo);

    Notificacion n = gestor.notificarAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "ups", null);

    assertEquals(EstadoEnvio.NO_APLICA, n.getEstadoEnvio());
    assertTrue(n.esParaAdministradores());
    assertEquals(1, repo.guardadas.size());
  }

  @Test
  void siNoHayNotificadorParaElTipoQuedaFallida() {
    RepositorioEnMemoria repo = new RepositorioEnMemoria();
    GestorNotificaciones gestor = new GestorNotificaciones(List.of(), repo);

    Notificacion n = gestor.notificar(
        donanteAna(), TipoNotificacion.CAMBIO_ESTADO, "hola", null);

    assertEquals(EstadoEnvio.FALLIDA, n.getEstadoEnvio());
    assertTrue(n.getDetalleError().contains("notificador"));
  }

  @Test
  void sinDestinatarioTiraDatosInvalidos() {
    GestorNotificaciones gestor =
        new GestorNotificaciones(List.of(), new RepositorioEnMemoria());
    assertThrows(DatosInvalidosException.class,
        () -> gestor.notificar((Donante) null, TipoNotificacion.CAMBIO_ESTADO, "hola", null));
  }

  // === Helpers ===

  private Donante donanteAna() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "12345678", Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    return new Donante(ana);
  }

  private static class NotificadorFalso implements Notificador {
    private final TipoContacto tipo;
    private final boolean falla;
    final List<String> recibidos = new ArrayList<>();

    NotificadorFalso(TipoContacto tipo, boolean falla) {
      this.tipo = tipo;
      this.falla = falla;
    }

    @Override
    public void notificar(String destino, String mensaje) {
      if (falla) {
        throw new RuntimeException("SMTP caido");
      }
      recibidos.add(destino + "|" + mensaje);
    }

    @Override
    public TipoContacto tipoAtendido() {
      return tipo;
    }
  }

  private static class RepositorioEnMemoria extends RepositorioNotificaciones {
    final List<Notificacion> guardadas = new ArrayList<>();

    @Override
    public void guardar(Notificacion notificacion) {
      guardadas.add(notificacion);
    }
  }
}
