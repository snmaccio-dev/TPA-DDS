package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.asignacion.Algoritmo;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPropuestas;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
}
