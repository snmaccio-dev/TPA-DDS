package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.SegmentadorDonaciones;
import donatrack.donaciones.domain.notificacion.DonacionObserver;
import donatrack.donaciones.domain.notificacion.NotificacionesDeDonacionObserver;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DonacionesServiceTest {

  private final RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);
  private final RepositorioSubcategorias repoSubcategorias = mock(RepositorioSubcategorias.class);

  @Test
  public void conObserversEnganchaElObserverDeNotificaciones() {
    DonacionesService service = nuevoService();
    Donacion donacion = donacionDeSillas();

    service.conObservers(donacion);

    List<DonacionObserver> observers = observersDe(donacion);
    assertEquals(1, observers.size());
    assertEquals(NotificacionesDeDonacionObserver.class, observers.get(0).getClass());
  }

  @Test
  public void conObserversEsIdempotentePorTipo() {
    DonacionesService service = nuevoService();
    Donacion donacion = donacionDeSillas();

    service.conObservers(donacion);
    service.conObservers(donacion);
    service.conObservers(donacion);

    assertEquals(1, observersDe(donacion).size(),
        "Llamar varias veces conObservers sobre la misma donacion no debe duplicar el observer.");
  }

  @Test
  public void crearConUnaSubcategoriaInexistenteDa404() {
    when(repoSubcategorias.buscarPorNombre("Inexistente")).thenReturn(Optional.empty());
    DonacionesService service = nuevoService();

    RecursoInexistenteException error = assertThrows(RecursoInexistenteException.class, () ->
        service.crear(donante(), List.of(
            new DatosBien("Silla", "Inexistente", 1, "USADO", null)), "desc", null));

    assertEquals("No existe la subcategoría 'Inexistente'.", error.getMessage());
    verifyNoInteractions(repoDonaciones);
  }

  @Test
  public void crearConUnaCondicionInvalidaDa400() {
    when(repoSubcategorias.buscarPorNombre("Sillas")).thenReturn(Optional.of(sillas()));
    DonacionesService service = nuevoService();

    assertThrows(DatosInvalidosException.class, () ->
        service.crear(donante(), List.of(
            new DatosBien("Silla", "Sillas", 1, "ROTO", null)), "desc", null));

    verifyNoInteractions(repoDonaciones);
  }

  @Test
  public void crearConUnaFechaDeVencimientoInvalidaDa400() {
    when(repoSubcategorias.buscarPorNombre("Sillas")).thenReturn(Optional.of(sillas()));
    DonacionesService service = nuevoService();

    assertThrows(DatosInvalidosException.class, () ->
        service.crear(donante(), List.of(
            new DatosBien("Silla", "Sillas", 1, "USADO", "31/12/2026")), "desc", null));

    verifyNoInteractions(repoDonaciones);
  }

  @Test
  public void subcategoriaPorNombreInexistenteDa404() {
    when(repoSubcategorias.buscarPorNombre("Nada")).thenReturn(Optional.empty());

    assertThrows(RecursoInexistenteException.class, () ->
        nuevoService().subcategoriaPorNombre("Nada"));
  }

  // === Helpers ===

  private DonacionesService nuevoService() {
    return new DonacionesService(
        repoDonaciones,
        repoSubcategorias,
        mock(RepositorioPropuestas.class),
        mock(RepositorioNotificaciones.class),
        new SegmentadorDonaciones(),
        mock(NotificacionesService.class),
        "http://web/donante",
        "http://web/beneficiario"
    );
  }

  private Donante donante() {
    return new Donante(new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO));
  }

  private Subcategoria sillas() {
    return new Subcategoria("Sillas", new Categoria("Mobiliario"), Unidades.UNIDADES);
  }

  private Donacion donacionDeSillas() {
    Bien silla = new Bien("Silla", sillas(), 1, CondicionBien.USADO);
    return new Donacion(List.of(silla), donante(), "demo");
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
