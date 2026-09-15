package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.service.GestorNotificaciones;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class NotificadorDonacionObserverTest {

  @Test
  public void onCambioEstadoArmaElMensajeYLlamaAlServicio() {
    Donante donante = donanteAna();
    GestorNotificaciones gestorFalso = mock(GestorNotificaciones.class);
    NotificadorDonacionObserver observer = new NotificadorDonacionObserver(donante, gestorFalso);

    observer.onCambioEstado(donacionDeSillas(donante), "EN_DEPOSITO", "ASIGNACION_REALIZADA");

    String mensajeEsperado = "Su donacion de [Sillas] cambio de estado: EN_DEPOSITO → ASIGNACION_REALIZADA";
    verify(gestorFalso, times(1)).notificar(donante.getPersona(), mensajeEsperado);
  }

  private Donante donanteAna() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "111", Genero.FEMENINO);
    ana.setUsuario(new Usuario("ana.perez", "***"));
    return new Donante(ana);
  }

  private Donacion donacionDeSillas(Donante donante) {
    Subcategoria subcategoria = new Subcategoria("Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);
    return new Donacion(
        List.of(new Bien("Silla", subcategoria, 1, CondicionBien.NUEVO)),
        donante,
        "Silla suelta"
    );
  }
}