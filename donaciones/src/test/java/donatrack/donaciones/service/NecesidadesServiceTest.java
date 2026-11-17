package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioNecesidades;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class NecesidadesServiceTest {

  private final RepositorioNecesidades repositorio = mock(RepositorioNecesidades.class);
  private final DonacionesService donacionesService = mock(DonacionesService.class);
  private final NecesidadesService service = new NecesidadesService(repositorio, donacionesService);

  @Test
  void crearConUnTipoInvalidoDa400() {
    assertThrows(DatosInvalidosException.class, () ->
        service.crear(beneficiaria(), "PERMANENTE", "Bancos", 10, "Bancos escolares", null));

    verifyNoInteractions(repositorio);
  }

  @Test
  void crearSinTipoDa400() {
    assertThrows(DatosInvalidosException.class, () ->
        service.crear(beneficiaria(), null, "Bancos", 10, "Bancos escolares", null));

    verifyNoInteractions(repositorio);
  }

  @Test
  void crearUnaRecurrenteConUnPeriodoInvalidoDa400() {
    assertThrows(DatosInvalidosException.class, () ->
        service.crear(beneficiaria(), "RECURRENTE", "Fideos", 100, "Fideos secos", "ANUAL"));

    verifyNoInteractions(repositorio);
  }

  @Test
  void crearUnaRecurrenteConElPeriodoVacioDa400() {
    assertThrows(DatosInvalidosException.class, () ->
        service.crear(beneficiaria(), "RECURRENTE", "Fideos", 100, "Fideos secos", ""));

    verifyNoInteractions(repositorio);
  }

  private Beneficiaria beneficiaria() {
    return new Beneficiaria(new PersonaJuridica(
        "30-11111111-1", "Escuela", TipoOrganizacion.INSTITUCION, "Educacion"));
  }
}
