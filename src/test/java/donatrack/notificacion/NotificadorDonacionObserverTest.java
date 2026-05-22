package donatrack.notificacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import org.junit.jupiter.api.Test;
import java.util.List;

// Importaciones de Mockito
import static org.mockito.Mockito.*;

class NotificadorDonacionObserverTest {

  @Test
  void onCambioEstadoArmaElMensajeYLlamaAlServicio() {
    // Arrange
    PersonaHumana donante = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    Subcategoria subcategoria = new Subcategoria("Sillas");
    Donacion donacion = new Donacion(List.of(new Bien("Silla", subcategoria, Unidades.UNIDADES)), subcategoria);

    ServicioNotificaciones servicioFalso = mock(ServicioNotificaciones.class);

    NotificadorDonacionObserver observer = new NotificadorDonacionObserver(donante, servicioFalso);

    // Act
    observer.onCambioEstado(donacion, "EN_DEPOSITO", "ASIGNACION_REALIZADA");

    // Assert
    String mensajeEsperado = "Su donacion de [Sillas] cambio de estado: EN_DEPOSITO → ASIGNACION_REALIZADA";
    verify(servicioFalso, times(1)).notificar(donante, mensajeEsperado);
  }
}
