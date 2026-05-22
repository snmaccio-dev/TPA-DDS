package donatrack.notificacion;

import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicioNotificacionesTest {

  @Test
  void notificarFallaSilenciosamenteSiLaPersonaNoTieneContacto() {
    // Arrange: Creamos un donante sin agregarle MedioContacto
    PersonaHumana donanteSinContacto = new PersonaHumana("Juan", "Perez", 30, "111", Genero.MASCULINO);
    ServicioNotificaciones servicio = new ServicioNotificaciones();

    // Act & Assert: Verificamos que el sistema no lance NullPointerException al intentar enviar
    assertDoesNotThrow(() -> {
      servicio.notificar(donanteSinContacto, "Mensaje de prueba");
    }, "El servicio debería manejar correctamente donantes sin medios de contacto");
  }

  @Test
  void notificarPorMedioEligeLaEstrategiaCorrectaSinExcepciones() {
    ServicioNotificaciones servicio = new ServicioNotificaciones();

    // Act & Assert: Verificamos que se resuelva la estrategia de EMAIL correctamente
    assertDoesNotThrow(() -> {
      servicio.notificarPorMedio("test@mail.com", TipoContacto.EMAIL, "Prueba Strategy");
    });
  }
}
