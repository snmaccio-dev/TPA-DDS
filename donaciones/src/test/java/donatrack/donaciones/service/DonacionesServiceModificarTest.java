package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.DatosModificacionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.SegmentadorDonaciones;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DonacionesServiceModificarTest implements WithSimplePersistenceUnit {

  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();
  private final RepositorioPropuestas repoPropuestas = new RepositorioPropuestas();

  @Test
  void modificarPersisteLosCambiosYBorraLaPropuesta() {
    Donacion donacion = donacionConPropuesta();
    long id = donacion.getId();
    long bienId = donacion.getBienes().get(0).getId();
    entityManager().clear();

    Donacion modificada = nuevoService().modificar(id, "Sillas de roble", "http://fotos/1.jpg",
        List.of(new DatosModificacionBien(bienId, "Silla de roble", 6.0)));

    assertEquals("Sillas de roble", modificada.getDescripcion());
    entityManager().clear();
    Donacion recargada = repoDonaciones.buscarPorId(id).orElseThrow();
    assertEquals("Sillas de roble", recargada.getDescripcion());
    assertEquals("http://fotos/1.jpg", recargada.getFotoUrl());
    Bien bien = recargada.getBienes().get(0);
    assertEquals("Silla de roble", bien.getDescripcion());
    assertEquals(6.0, bien.getCantidad());
    assertEquals(CondicionBien.USADO, bien.getCondicion());
    assertTrue(repoPropuestas.buscarPorDonacion(id).isEmpty(),
        "La propuesta de asignacion se tiene que borrar para que el matchmaking la recalcule.");
  }

  @Test
  void conDatosInvalidosNoPersisteNadaNiBorraLaPropuesta() {
    Donacion donacion = donacionConPropuesta();
    long id = donacion.getId();
    long bienId = donacion.getBienes().get(0).getId();
    entityManager().clear();

    assertThrows(DatosInvalidosException.class, () -> nuevoService().modificar(id, "Otra", null,
        List.of(new DatosModificacionBien(bienId, "Silla", 0.0))));

    entityManager().clear();
    Donacion recargada = repoDonaciones.buscarPorId(id).orElseThrow();
    assertEquals("Sillas viejas", recargada.getDescripcion());
    assertEquals(2.0, recargada.getBienes().get(0).getCantidad());
    assertFalse(repoPropuestas.buscarPorDonacion(id).isEmpty());
  }

  @Test
  void unaDonacionInexistenteDa404() {
    assertThrows(RecursoInexistenteException.class, () ->
        nuevoService().modificar(Long.MAX_VALUE, "desc", null, List.of()));
  }

  private Donacion donacionConPropuesta() {
    Donacion[] creada = new Donacion[1];
    withTransaction(() -> {
      Categoria categoria = new Categoria(DatosDeTest.textoUnico("Mobiliario"));
      entityManager().persist(categoria);
      Subcategoria sillas =
          new Subcategoria(DatosDeTest.textoUnico("Sillas"), categoria, Unidades.UNIDADES);
      entityManager().persist(sillas);
      PersonaHumana persona = new PersonaHumana(
          "Ana", "Perez", 30, DatosDeTest.documentoUnico(), Genero.FEMENINO);
      RepositorioPersonas.getInstance().guardar(persona);
      Donante donante = new Donante(persona);
      entityManager().persist(donante);
      creada[0] = Donacion.crear(
          List.of(new Bien("Silla", sillas, 2, CondicionBien.USADO)), donante, "Sillas viejas");
      repoDonaciones.guardar(creada[0]);
      repoPropuestas.guardar(new PropuestaAsignacion(
          creada[0].getId(), List.of(), LocalDateTime.now(), false));
    });
    return creada[0];
  }

  private DonacionesService nuevoService() {
    return new DonacionesService(
        repoDonaciones,
        new RepositorioSubcategorias(),
        repoPropuestas,
        new RepositorioNotificaciones(),
        new SegmentadorDonaciones(),
        mock(NotificacionesService.class),
        "http://web/donante",
        "http://web/beneficiario");
  }
}
