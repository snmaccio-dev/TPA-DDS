package donatrack.donaciones.domain.donacion.asignacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class AlgoritmosDeAsignacionTest {

  private final Donacion donacion = mock(Donacion.class);

  // === Regla de compatibilidad (Beneficiaria / Necesidad) ===

  @Test
  void laNecesidadSeComparaPorIdDeSubcategoriaYNoPorInstancia() {
    // Dos instancias distintas de la misma fila, como las cargarian dos EntityManagers.
    Subcategoria mesasDeLaNecesidad = subcategoria(7L);
    Subcategoria mesasDeLaDonacion = subcategoria(7L);
    Subcategoria sillas = subcategoria(8L);
    Beneficiaria escuela = beneficiaria("Escuela");
    new NecesidadExtraordinaria("Mesas", 12, mesasDeLaNecesidad, escuela);

    assertTrue(escuela.tieneNecesidadDe(mesasDeLaDonacion));
    assertEquals(12, escuela.cantidadNecesitadaDe(mesasDeLaDonacion));
    assertFalse(escuela.tieneNecesidadDe(sillas));
    assertEquals(0, escuela.cantidadNecesitadaDe(sillas));
  }

  @Test
  void laCantidadNecesitadaSumaTodasLasNecesidadesDeEsaSubcategoria() {
    Subcategoria mesas = subcategoria(7L);
    Beneficiaria escuela = beneficiaria("Escuela");
    new NecesidadExtraordinaria("Mesas comedor", 5, mesas, escuela);
    new NecesidadExtraordinaria("Mesas aulas", 3, mesas, escuela);
    new NecesidadExtraordinaria("Sillas", 40, subcategoria(8L), escuela);

    assertEquals(8, escuela.cantidadNecesitadaDe(subcategoria(7L)));
  }

  // === Compatibilidad semantica ===

  @Test
  void compatibilidadOrdenaPorPuntajeYDescartaLasDePuntajeCero() {
    Beneficiaria poco = candidata(1L, 5, 0);
    Beneficiaria mucho = candidata(2L, 40, 0);
    Beneficiaria nada = candidata(3L, 0, 0);
    Beneficiaria medio = candidata(4L, 20, 0);

    List<Beneficiaria> ranking = new CompatibilidadSemantica()
        .matchmaking(donacion, List.of(poco, mucho, nada, medio));

    assertEquals(List.of(mucho, medio, poco), ranking);
  }

  @Test
  void compatibilidadDesempataPorIdYDevuelveHastaDiez() {
    List<Beneficiaria> doce = LongStream.rangeClosed(1, 12)
        .mapToObj(id -> candidata(13 - id, 10, 0))
        .toList();

    List<Beneficiaria> ranking = new CompatibilidadSemantica().matchmaking(donacion, doce);

    assertEquals(10, ranking.size());
    assertEquals(LongStream.rangeClosed(1, 10).boxed().toList(),
        ranking.stream().map(Beneficiaria::getId).toList());
  }

  // === Prioridad a sub-atendidos ===

  @Test
  void prioridadOrdenaPorMenosDonacionesDelTrimestreYDesempataPorId() {
    Beneficiaria muyAtendida = candidata(1L, 10, 6);
    Beneficiaria sinDonaciones = candidata(5L, 10, 0);
    Beneficiaria otraSinDonaciones = candidata(2L, 10, 0);
    Beneficiaria algoAtendida = candidata(3L, 10, 2);

    List<Beneficiaria> ranking = new PrioridadSubatendidos().matchmaking(
        donacion, List.of(muyAtendida, sinDonaciones, otraSinDonaciones, algoAtendida));

    assertEquals(List.of(otraSinDonaciones, sinDonaciones, algoAtendida, muyAtendida), ranking);
  }

  @Test
  void prioridadDevuelveHastaDiez() {
    List<Beneficiaria> doce = LongStream.rangeClosed(1, 12)
        .mapToObj(id -> candidata(id, 10, id))
        .toList();

    assertEquals(10, new PrioridadSubatendidos().matchmaking(donacion, doce).size());
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

  // Los algoritmos solo miran id, puntaje y donaciones del trimestre.
  private static Beneficiaria candidata(long id, int puntaje, long donacionesTrimestre) {
    Beneficiaria beneficiaria = mock(Beneficiaria.class);
    when(beneficiaria.getId()).thenReturn(id);
    when(beneficiaria.cantidadNecesitadaDe(any())).thenReturn(puntaje);
    when(beneficiaria.getCantidadDonacionesUltimoTrimestre()).thenReturn(donacionesTrimestre);
    return beneficiaria;
  }
}
