package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.repository.RepositorioDonantes;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DonantesServiceTest implements SimplePersistenceTest {

  private final donatrack.donaciones.repository.RepositorioPersonas repoPersonas =
      donatrack.donaciones.repository.RepositorioPersonas.getInstance();

  @Test
  void siLaNotificacionSaleFallidaNoRegistraElAvisoDeInactividad() {
    String documento = DatosDeTest.documentoUnico();
    Donante donante = donantePersistidoViejo(
        documento, "Ana", DatosDeTest.emailUnico("ana-" + documento));

    NotificacionesService notificacionesService = mock(NotificacionesService.class);
    Notificacion fallida = Notificacion.paraDonante(
        donante, TipoNotificacion.AUSENCIA_PLATAFORMA, "x", null);
    fallida.marcarFallida("sin canal");
    when(notificacionesService.notificarAusenciaPlataforma(any())).thenReturn(fallida);

    RepositorioDonantes repoDonantes = new RepositorioDonantes();
    DonantesService service = new DonantesService(repoDonantes, notificacionesService);

    ResultadoCorrida resultado = service.avisarInactivos();

    assertEquals(0, resultado.exitosas(),
        "Si la notificacion quedo FALLIDA no cuenta como avisado.");
    assertEquals(1, resultado.cantidadDeFallas());

    // Despues del gestor la transaccion ya esta commiteada y cerrada; solo
    // podemos limpiar la cache del EM y releer desde la base. Un flush() aca
    // tiraria IllegalStateException por "Transaction not active".
    entityManager().clear();
    Donante recargado = entityManager().find(Donante.class, donante.getId());
    assertNull(recargado.getFechaUltimoAvisoInactividad(),
        "No se tiene que haber registrado el aviso para reintentarlo despues.");
  }

  @Test
  void siLaNotificacionSaleEnviadaRegistraElAviso() {
    String documento = DatosDeTest.documentoUnico();
    Donante donante = donantePersistidoViejo(
        documento, "Luis", DatosDeTest.emailUnico("luis-" + documento));

    NotificacionesService notificacionesService = mock(NotificacionesService.class);
    Notificacion enviada = Notificacion.paraDonante(
        donante, TipoNotificacion.AUSENCIA_PLATAFORMA, "x", null);
    enviada.marcarEnviada(TipoContacto.EMAIL, "luis@mail");
    when(notificacionesService.notificarAusenciaPlataforma(any())).thenReturn(enviada);

    RepositorioDonantes repoDonantes = new RepositorioDonantes();
    DonantesService service = new DonantesService(repoDonantes, notificacionesService);

    ResultadoCorrida resultado = service.avisarInactivos();

    assertEquals(1, resultado.exitosas());
    assertEquals(0, resultado.cantidadDeFallas());

    // Idem comentario arriba: sin flush() despues del service.
    entityManager().clear();
    Donante recargado = entityManager().find(Donante.class, donante.getId());
    assertEquals(EstadoEnvio.ENVIADA, enviada.getEstadoEnvio());
    // La fecha se seteo durante el withTransaction del service.
    assertEquals(LocalDate.now(), recargado.getFechaUltimoAvisoInactividad());
  }

  // === Helpers ===

  // Persiste el donante y lo envejece dentro de la transaccion que abre
  // SimplePersistenceTest (antes de llamar al service). flush para que el
  // update por JPQL vea la fila recien persistida.
  private Donante donantePersistidoViejo(String documento, String nombre, String email) {
    PersonaHumana persona = new PersonaHumana(nombre, "Test", 30, documento, Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    entityManager().persist(donante);
    entityManager().flush();
    entityManager().createQuery(
        "update Donante d set d.ultimaInteraccion = :vieja where d.id = :id")
        .setParameter("vieja", LocalDate.now().minusDays(60))
        .setParameter("id", donante.getId())
        .executeUpdate();
    entityManager().clear();
    return entityManager().find(Donante.class, donante.getId());
  }
}
