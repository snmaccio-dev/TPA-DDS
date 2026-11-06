package donatrack.donaciones.service;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.DonacionObserver;
import donatrack.donaciones.domain.notificacion.NotificacionesDeDonacionObserver;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPropuestas;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class GestorDonacionesTest {

  @Test
  public void conObserversEnganchaElObserverDeNotificaciones() {
    GestorDonaciones gestor = new GestorDonaciones(
        mock(GestorNotificaciones.class),
        mock(RepositorioPropuestas.class),
        mock(RepositorioNotificaciones.class),
        "http://web/donante",
        "http://web/beneficiario"
    );
    Donacion donacion = donacionDeSillas();

    gestor.conObservers(donacion);

    List<DonacionObserver> observers = observersDe(donacion);
    assertEquals(1, observers.size());
    assertEquals(NotificacionesDeDonacionObserver.class, observers.get(0).getClass());
  }

  @Test
  public void conObserversEsIdempotentePorTipo() {
    GestorDonaciones gestor = new GestorDonaciones(
        mock(GestorNotificaciones.class),
        mock(RepositorioPropuestas.class),
        mock(RepositorioNotificaciones.class),
        "http://web/donante",
        "http://web/beneficiario"
    );
    Donacion donacion = donacionDeSillas();

    gestor.conObservers(donacion);
    gestor.conObservers(donacion);
    gestor.conObservers(donacion);

    assertEquals(1, observersDe(donacion).size(),
        "Llamar varias veces conObservers sobre la misma donacion no debe duplicar el observer.");
  }

  // === Helpers ===

  private Donacion donacionDeSillas() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);
    Donante donante = new Donante(ana);
    Subcategoria sillas = new Subcategoria(
        "Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);
    Bien silla = new Bien("Silla", sillas, 1, CondicionBien.USADO);
    return new Donacion(List.of(silla), donante, "demo");
  }

  @SuppressWarnings("unchecked")
  private List<DonacionObserver> observersDe(Donacion donacion) {
    try {
      Field campo = Donacion.class.getDeclaredField("observers");
      campo.setAccessible(true);
      return (List<DonacionObserver>) campo.get(donacion);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
