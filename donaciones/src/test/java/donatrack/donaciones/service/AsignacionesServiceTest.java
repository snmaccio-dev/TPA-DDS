package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.Algoritmo;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPropuestas;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AsignacionesServiceTest {

  private final RepositorioEntidades repoEntidades = mock(RepositorioEntidades.class);
  private final RepositorioPropuestas repoPropuestas = mock(RepositorioPropuestas.class);
  private final DonacionesService donacionesService = mock(DonacionesService.class);
  private final AsignacionesService service = new AsignacionesService(
      mock(Algoritmo.class),
      mock(Algoritmo.class),
      mock(RepositorioDonaciones.class),
      repoEntidades,
      repoPropuestas,
      donacionesService);

  @Test
  void confirmarDestinoConUnaBeneficiariaInexistenteDa404() {
    when(repoEntidades.buscarPorId(77L)).thenReturn(Optional.empty());

    RecursoInexistenteException error = assertThrows(RecursoInexistenteException.class, () ->
        service.confirmarDestino(5L, 77L));

    assertEquals("No existe la beneficiaria con id 77", error.getMessage());
    verifyNoInteractions(donacionesService, repoPropuestas);
  }

  @Test
  void propuestaDeUnaDonacionSinPropuestaDa404() {
    when(repoPropuestas.buscarPorDonacion(5L)).thenReturn(Optional.empty());

    RecursoInexistenteException error = assertThrows(RecursoInexistenteException.class, () ->
        service.propuestaDe(5L));

    assertEquals("No hay propuesta generada para la donación 5", error.getMessage());
  }

  @Test
  void confirmarDestinoRechazaUnaBeneficiariaQueNoEstaEnLaPropuesta() {
    Beneficiaria propuesta = beneficiariaConId(1L);
    Beneficiaria otraPropuesta = beneficiariaConId(2L);
    Beneficiaria fueraDeLaPropuesta = beneficiariaConId(3L);

    when(repoEntidades.buscarPorId(3L)).thenReturn(Optional.of(fueraDeLaPropuesta));
    when(donacionesService.buscar(5L)).thenReturn(mock(Donacion.class));
    when(repoPropuestas.buscarPorDonacion(5L)).thenReturn(Optional.of(
        PropuestaAsignacion.desdeRankings(
            5L, List.of(propuesta), List.of(otraPropuesta), LocalDateTime.now())));

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        service.confirmarDestino(5L, 3L));

    assertEquals(
        "No se puede asignar la donación: la beneficiaria seleccionada no está "
            + "entre las candidatas propuestas por los algoritmos para esta donación.",
        error.getMessage());
    verify(repoPropuestas, never()).eliminar(5L);
  }

  @Test
  void confirmarDestinoSinPropuestaGeneradaSeRechaza() {
    // Se crea antes: armar el mock dentro de otro when(...) deja el stubbing a medias.
    Beneficiaria beneficiaria = beneficiariaConId(3L);
    when(repoEntidades.buscarPorId(3L)).thenReturn(Optional.of(beneficiaria));
    when(donacionesService.buscar(5L)).thenReturn(mock(Donacion.class));
    when(repoPropuestas.buscarPorDonacion(5L)).thenReturn(Optional.empty());

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        service.confirmarDestino(5L, 3L));

    assertEquals(
        "No se puede asignar la donación 5: todavía no tiene una propuesta de "
            + "asignación generada.",
        error.getMessage());
  }

  private static Beneficiaria beneficiariaConId(long id) {
    Beneficiaria beneficiaria = mock(Beneficiaria.class);
    when(beneficiaria.getId()).thenReturn(id);
    return beneficiaria;
  }
}
