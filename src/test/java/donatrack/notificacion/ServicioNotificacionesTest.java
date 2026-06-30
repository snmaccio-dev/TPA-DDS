package donatrack.notificacion;

import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import org.junit.jupiter.api.Test;
import gestion.GestorPersonas;

import static org.junit.jupiter.api.Assertions.*;

public class ServicioNotificacionesTest {

  @Test
  public void notificarFallaSilenciosamenteSiLaPersonaNoTieneContacto() {
    GestorPersonas.ServicioNotificaciones servicio = new GestorPersonas.ServicioNotificaciones();

    assertDoesNotThrow(() -> {
      servicio.notificar(donanteSinContacto(), "Mensaje de prueba");
    });
  }

  @Test
  public void notificarPorMedioEligeLaEstrategiaCorrectaSinExcepciones() {
    GestorPersonas.ServicioNotificaciones servicio = new GestorPersonas.ServicioNotificaciones();

    assertDoesNotThrow(() -> {
      servicio.notificarPorMedio("test@mail.com", TipoContacto.EMAIL, "Prueba Strategy");
    });
  }

  private PersonaHumana donanteSinContacto() {
    return new PersonaHumana("Martin", "Friedrich", 23, "44510667", Genero.MASCULINO);
  }
}
