package donatrack.donaciones.service;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestorDonantesTest implements SimplePersistenceTest {

  private final donatrack.donaciones.repository.RepositorioPersonas repoPersonas =
      donatrack.donaciones.repository.RepositorioPersonas.getInstance();

  @Test
  void siLaNotificacionSaleFallidaNoRegistraElAvisoDeInactividad() {
    // Donante con una ultima interaccion vieja, para que entre en inactivos().
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "99998888", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    repoPersonas.guardar(persona);

    Donante donante = new Donante(persona);
    // Lo envejecemos directamente en la BD para que queRequierenAvisoDeInactividad lo agarre.
    entityManager().getTransaction().begin();
    entityManager().persist(donante);
    entityManager().getTransaction().commit();

    entityManager().getTransaction().begin();
    entityManager().createQuery(
        "update Donante d set d.ultimaInteraccion = :vieja where d.id = :id")
        .setParameter("vieja", LocalDate.now().minusDays(60))
        .setParameter("id", donante.getId())
        .executeUpdate();
    entityManager().getTransaction().commit();
    entityManager().clear();

    GestorNotificaciones gestorNotificaciones = mock(GestorNotificaciones.class);
    Notificacion fallida = Notificacion.paraDonante(
        donante, TipoNotificacion.AUSENCIA_PLATAFORMA, "x", null);
    fallida.marcarFallida("sin canal");
    when(gestorNotificaciones.notificarAusenciaPlataforma(any())).thenReturn(fallida);

    RepositorioDonantes repoDonantes = new RepositorioDonantes();
    GestorDonantes gestor = new GestorDonantes(repoDonantes, gestorNotificaciones);

    ResultadoCorrida resultado = gestor.avisarInactivos();

    assertEquals(0, resultado.exitosas(),
        "Si la notificacion quedo FALLIDA no cuenta como avisado.");
    assertEquals(1, resultado.cantidadDeFallas());

    entityManager().clear();
    Donante recargado = entityManager().find(Donante.class, donante.getId());
    assertNull(recargado.getFechaUltimoAvisoInactividad(),
        "No se tiene que haber registrado el aviso para reintentarlo despues.");
  }

  @Test
  void siLaNotificacionSaleEnviadaRegistraElAviso() {
    PersonaHumana persona = new PersonaHumana("Luis", "Garcia", 45, "77776666", Genero.MASCULINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "luis@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    entityManager().getTransaction().begin();
    entityManager().persist(donante);
    entityManager().getTransaction().commit();

    entityManager().getTransaction().begin();
    entityManager().createQuery(
        "update Donante d set d.ultimaInteraccion = :vieja where d.id = :id")
        .setParameter("vieja", LocalDate.now().minusDays(60))
        .setParameter("id", donante.getId())
        .executeUpdate();
    entityManager().getTransaction().commit();
    entityManager().clear();

    GestorNotificaciones gestorNotificaciones = mock(GestorNotificaciones.class);
    Notificacion enviada = Notificacion.paraDonante(
        donante, TipoNotificacion.AUSENCIA_PLATAFORMA, "x", null);
    enviada.marcarEnviada(TipoContacto.EMAIL, "luis@mail.com");
    when(gestorNotificaciones.notificarAusenciaPlataforma(any())).thenReturn(enviada);

    RepositorioDonantes repoDonantes = new RepositorioDonantes();
    GestorDonantes gestor = new GestorDonantes(repoDonantes, gestorNotificaciones);

    ResultadoCorrida resultado = gestor.avisarInactivos();

    assertEquals(1, resultado.exitosas());
    assertEquals(0, resultado.cantidadDeFallas());

    entityManager().clear();
    Donante recargado = entityManager().find(Donante.class, donante.getId());
    assertEquals(EstadoEnvio.ENVIADA, enviada.getEstadoEnvio());
    // La fecha se seteo durante el withTransaction del gestor.
    assertEquals(LocalDate.now(), recargado.getFechaUltimoAvisoInactividad());
  }

  // Lista aqui solo para cubrir la variable "List" que usan los tests al tipar; no se usa.
  @SuppressWarnings("unused")
  private List<Donante> noUsar() {
    return List.of();
  }
}
