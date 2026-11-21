package donatrack.donaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.CompatibilidadSemantica;
import donatrack.donaciones.domain.donacion.asignacion.PrioridadSubatendidos;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPropuestas;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

// Matchmaking con los algoritmos reales: solo compiten las entidades con una necesidad de
// la subcategoria donada, y la confirmacion usa la misma regla.
class AsignacionesServiceMatchmakingTest {

  private static final long DONACION_ID = 5L;

  private final RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);
  private final RepositorioEntidades repoEntidades = mock(RepositorioEntidades.class);
  private final RepositorioPropuestas repoPropuestas = mock(RepositorioPropuestas.class);
  private final DonacionesService donacionesService = mock(DonacionesService.class);
  private final AsignacionesService service = new AsignacionesService(
      new CompatibilidadSemantica(),
      new PrioridadSubatendidos(),
      repoDonaciones,
      repoEntidades,
      repoPropuestas,
      donacionesService);

  // Instancias distintas con el mismo id: la comparacion tiene que ser por id.
  private final Subcategoria mesasDeLaDonacion = subcategoria(7L);
  private final Subcategoria mesasDeLasNecesidades = subcategoria(7L);
  private final Subcategoria sillas = subcategoria(8L);

  @Test
  void soloSonCandidatasLasEntidadesConUnaNecesidadDeLaSubcategoria() {
    Beneficiaria conMesas = beneficiariaQueNecesita("Escuela", mesasDeLasNecesidades, 10);
    Beneficiaria otraConMesas = beneficiariaQueNecesita("Comedor", mesasDeLasNecesidades, 30);
    Beneficiaria conSillas = beneficiariaQueNecesita("Club", sillas, 50);
    Beneficiaria sinNecesidades = beneficiaria("Hogar");
    when(repoEntidades.todas()).thenReturn(
        List.of(conMesas, conSillas, otraConMesas, sinNecesidades));
    Donacion donacion = donacionEnDeposito(mesasDeLaDonacion);
    when(donacionesService.buscar(DONACION_ID)).thenReturn(donacion);

    List<Beneficiaria> candidatas = service.ejecutarAsignacion(DONACION_ID);

    // Hay coincidencia (las dos estan en ambos rankings), en el orden de compatibilidad.
    assertEquals(List.of(otraConMesas, conMesas), candidatas);
  }

  @Test
  void siNingunaEntidadNecesitaLaSubcategoriaLaPropuestaQuedaSinCandidatas() {
    when(repoEntidades.todas()).thenReturn(List.of(
        beneficiariaQueNecesita("Club", sillas, 50), beneficiaria("Hogar")));
    when(donacionesService.buscar(DONACION_ID))
        .thenReturn(donacionEnDeposito(mesasDeLaDonacion));

    assertTrue(service.ejecutarAsignacion(DONACION_ID).isEmpty());
  }

  @Test
  void confirmarRechazaUnaCandidataQueNoTieneNecesidadCompatible() {
    // Propuesta vieja (generada con el bug) que incluye a una entidad sin necesidad.
    Beneficiaria conSillas = beneficiariaQueNecesita("Club", sillas, 50);
    when(repoEntidades.buscarPorId(3L)).thenReturn(Optional.of(conSillas));
    Donacion donacion = donacionEnDeposito(mesasDeLaDonacion);
    when(donacionesService.buscar(DONACION_ID)).thenReturn(donacion);
    when(repoPropuestas.buscarPorDonacion(DONACION_ID)).thenReturn(Optional.of(
        PropuestaAsignacion.desdeRankings(
            DONACION_ID, List.of(conSillas), List.of(conSillas), LocalDateTime.now())));

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
        () -> service.confirmarDestino(DONACION_ID, 3L));

    assertEquals("No se puede asignar la donación: la beneficiaria seleccionada "
        + "no tiene una necesidad compatible con su subcategoría.", error.getMessage());
    verify(donacion, never()).confirmarDestino(any());
  }

  @Test
  void laCorridaRegeneraLaPropuestaAunqueLaDonacionYaTuvieraUna() {
    Beneficiaria conMesas = beneficiariaQueNecesita("Escuela", mesasDeLasNecesidades, 10);
    Beneficiaria conSillas = beneficiariaQueNecesita("Club", sillas, 50);
    when(repoEntidades.todas()).thenReturn(List.of(conMesas, conSillas));
    Donacion donacion = donacionEnDeposito(mesasDeLaDonacion);
    when(repoDonaciones.enEstado(EstadosPosiblesDonacion.EN_DEPOSITO))
        .thenReturn(List.of(donacion));
    // La que habia tenia a todas las entidades (bug anterior).
    when(repoPropuestas.buscarPorDonacion(DONACION_ID)).thenReturn(Optional.of(
        PropuestaAsignacion.desdeRankings(DONACION_ID, List.of(conMesas, conSillas),
            List.of(conMesas, conSillas), LocalDateTime.now())));

    ResultadoCorrida resultado = service.ejecutarMatchmakingProgramado();

    assertEquals(1, resultado.exitosas());
    ArgumentCaptor<PropuestaAsignacion> nueva = ArgumentCaptor.forClass(PropuestaAsignacion.class);
    InOrder orden = inOrder(repoPropuestas);
    orden.verify(repoPropuestas).eliminar(DONACION_ID);
    orden.verify(repoPropuestas).guardar(nueva.capture());
    assertEquals(List.of(conMesas), nueva.getValue().getCandidatas());
  }

  // === Helpers ===

  private static Subcategoria subcategoria(long id) {
    Subcategoria subcategoria = mock(Subcategoria.class);
    when(subcategoria.getId()).thenReturn(id);
    return subcategoria;
  }

  private static Beneficiaria beneficiaria(String razonSocial) {
    return new Beneficiaria(new PersonaJuridica(
        "30-11111111-1", razonSocial, TipoOrganizacion.ONG, "Rubro"));
  }

  private static Beneficiaria beneficiariaQueNecesita(String razonSocial,
                                                      Subcategoria subcategoria,
                                                      int cantidad) {
    Beneficiaria beneficiaria = beneficiaria(razonSocial);
    new NecesidadExtraordinaria("Necesidad", cantidad, subcategoria, beneficiaria);
    return beneficiaria;
  }

  private static Donacion donacionEnDeposito(Subcategoria subcategoria) {
    Donacion donacion = mock(Donacion.class);
    when(donacion.getId()).thenReturn(DONACION_ID);
    when(donacion.getEstado()).thenReturn(new EstadoDonacion(EstadosPosiblesDonacion.EN_DEPOSITO));
    when(donacion.getSubcategoria()).thenReturn(subcategoria);
    return donacion;
  }
}
