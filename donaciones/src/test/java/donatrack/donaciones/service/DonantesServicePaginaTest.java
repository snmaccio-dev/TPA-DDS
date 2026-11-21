package donatrack.donaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonantesServicePaginaTest {

  private final RepositorioDonantes repositorio = mock(RepositorioDonantes.class);
  private final DonantesService service = new DonantesService(
      repositorio,
      mock(RepositorioPersonas.class),
      mock(RepositorioNotificaciones.class),
      mock(RepositorioDonaciones.class),
      mock(RepositorioEntidades.class),
      mock(NotificacionesService.class),
      mock(UsuariosService.class));

  @Test
  void unTamanioMayorAlMaximoSeRecortaACien() {
    when(repositorio.contar("ana")).thenReturn(250L);
    when(repositorio.pagina("ana", 1, 100)).thenReturn(List.of());

    Pagina<Persona> pagina = service.pagina("ana", 1, 500);

    assertEquals(100, pagina.tamanio());
    assertEquals(250, pagina.total());
    assertEquals(3, pagina.totalPaginas());
    verify(repositorio).pagina("ana", 1, 100);
  }

  @Test
  void sinResultadosNoPideLaPagina() {
    when(repositorio.contar("nadie")).thenReturn(0L);

    Pagina<Persona> pagina = service.pagina("nadie", 1, 20);

    assertTrue(pagina.items().isEmpty());
    assertEquals(0, pagina.totalPaginas());
    verify(repositorio, never()).pagina(anyString(), anyInt(), anyInt());
  }

  @Test
  void unaPaginaMenorAUnoOUnTamanioInvalidoSeRechazan() {
    assertThrows(DatosInvalidosException.class, () -> service.pagina(null, 0, 20));
    assertThrows(DatosInvalidosException.class, () -> service.pagina(null, 1, 0));
  }
}
