package donatrack.notificacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import java.util.List;
import org.junit.jupiter.api.Test;
import gestion.GestorPersonas;

import static org.mockito.Mockito.*;

public class NotificadorDonacionObserverTest {

  @Test
  public void onCambioEstadoArmaElMensajeYLlamaAlServicio() {
    PersonaHumana donante = donanteAna();
    GestorPersonas.ServicioNotificaciones servicioFalso = mock(GestorPersonas.ServicioNotificaciones.class);
    NotificadorDonacionObserver observer = new NotificadorDonacionObserver(donante, servicioFalso);

    observer.onCambioEstado(donacionDeSillas(), "EN_DEPOSITO", "ASIGNACION_REALIZADA");

    String mensajeEsperado = "Su donacion de [Sillas] cambio de estado: EN_DEPOSITO → ASIGNACION_REALIZADA";
    verify(servicioFalso, times(1)).notificar(donante, mensajeEsperado);
  }

  private PersonaHumana donanteAna() {
    return new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
  }

  private Donacion donacionDeSillas() {
    Subcategoria subcategoria = new Subcategoria("Sillas");
    return new Donacion(List.of(new Bien("Silla", subcategoria, Unidades.UNIDADES)), subcategoria);
  }
}
