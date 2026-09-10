package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.Unidades;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.usuario.Usuario;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class NotificadorDonacionObserverTest {

  @Test
  public void onCambioEstadoArmaElMensajeYLlamaAlServicio() {
    Donante donante = donanteAna();
    Notificador servicioFalso = mock(Notificador.class);
    NotificadorDonacionObserver observer = new NotificadorDonacionObserver(donante, servicioFalso);

    observer.onCambioEstado(donacionDeSillas(donante), "EN_DEPOSITO", "ASIGNACION_REALIZADA");

    String mensajeEsperado = "Su donacion de [Sillas] cambio de estado: EN_DEPOSITO → ASIGNACION_REALIZADA";
    verify(servicioFalso, times(1)).notificar(donante.getPersona().getUsuario().getNombre(), mensajeEsperado);
  }

  private Donante donanteAna() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    ana.setUsuario(new Usuario("ana.perez", "***"));
    return new Donante(ana);
  }

  private Donacion donacionDeSillas(Donante donante) {
    Subcategoria subcategoria = new Subcategoria("Sillas", new Categoria("Mobiliario"));
    return new Donacion(
        List.of(new Bien("Silla", subcategoria, 1, Unidades.UNIDADES, CondicionBien.NUEVO)),
        donante,
        "Silla suelta"
    );
  }
}
