package donatrack.donaciones.domain.donacion.asignacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.controller.dto.PropuestaDTO;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

// La propuesta guarda el resultado por algoritmo (candidata + algoritmo + posicion) y
// PropuestaDTO lo expone: una lista de coincidencias, o las dos ejecuciones por separado.
class PropuestaAsignacionTest implements SimplePersistenceTest {

  private static final AtomicLong DONACION_ID = new AtomicLong(880_000L);

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioPropuestas repoPropuestas = new RepositorioPropuestas();

  @Test
  void conCoincidenciasSoloQuedanLasCoincidenciasEnOrden() {
    Beneficiaria a = beneficiaria("Comedor A");
    Beneficiaria b = beneficiaria("Hogar B");
    Beneficiaria c = beneficiaria("Escuela C");
    Beneficiaria d = beneficiaria("Club D");
    long donacionId = DONACION_ID.incrementAndGet();

    // Compatibilidad: A, B, C. Prioridad: D, C, A. Coinciden A y C.
    PropuestaDTO dto = guardarYReleer(PropuestaAsignacion.desdeRankings(
        donacionId, List.of(a, b, c), List.of(d, c, a), LocalDateTime.now()));

    assertTrue(dto.huboCoincidencias());
    assertEquals(1, dto.rankings().size());

    PropuestaDTO.RankingDTO coincidencias = dto.rankings().get(0);
    assertEquals("AMBOS", coincidencias.algoritmo());
    assertEquals("Coinciden ambos algoritmos", coincidencias.nombre());
    assertEquals(List.of("Comedor A", "Escuela C"), razonesSociales(coincidencias));
    assertEquals(List.of(1, 2), posiciones(coincidencias));
    assertEquals(a.getPersona().getCuit(), coincidencias.candidatas().get(0).cuit());
    assertEquals(2, dto.candidatas().size());
  }

  @Test
  void sinCoincidenciasSeGuardanLasDosEjecucionesConSuRanking() {
    Beneficiaria a = beneficiaria("Comedor A");
    Beneficiaria b = beneficiaria("Hogar B");
    Beneficiaria c = beneficiaria("Escuela C");
    long donacionId = DONACION_ID.incrementAndGet();

    PropuestaDTO dto = guardarYReleer(PropuestaAsignacion.desdeRankings(
        donacionId, List.of(a, b), List.of(c), LocalDateTime.now()));

    assertFalse(dto.huboCoincidencias());
    assertEquals(2, dto.rankings().size());

    PropuestaDTO.RankingDTO compatibilidad = dto.rankings().get(0);
    assertEquals("COMPATIBILIDAD_SEMANTICA", compatibilidad.algoritmo());
    assertEquals("Compatibilidad semántica", compatibilidad.nombre());
    assertEquals(List.of("Comedor A", "Hogar B"), razonesSociales(compatibilidad));
    assertEquals(List.of(1, 2), posiciones(compatibilidad));

    PropuestaDTO.RankingDTO prioridad = dto.rankings().get(1);
    assertEquals("PRIORIDAD_SUBATENDIDOS", prioridad.algoritmo());
    assertEquals("Prioridad a sub-atendidos", prioridad.nombre());
    assertEquals(List.of("Escuela C"), razonesSociales(prioridad));
    assertEquals(List.of(1), posiciones(prioridad));

    assertEquals(3, dto.candidatas().size());
  }

  @Test
  void esCandidataSoloParaLasEntidadesPropuestas() {
    Beneficiaria propuesta = beneficiaria("Propuesta");
    Beneficiaria afuera = beneficiaria("Afuera");

    PropuestaAsignacion resultado = PropuestaAsignacion.desdeRankings(
        DONACION_ID.incrementAndGet(), List.of(propuesta), List.of(), LocalDateTime.now());

    assertTrue(resultado.esCandidata(propuesta));
    assertFalse(resultado.esCandidata(afuera));
  }

  private PropuestaDTO guardarYReleer(PropuestaAsignacion propuesta) {
    repoPropuestas.guardar(propuesta);
    entityManager().flush();
    entityManager().clear();

    return PropuestaDTO.desde(
        repoPropuestas.buscarPorDonacion(propuesta.getDonacionId()).orElseThrow());
  }

  private Beneficiaria beneficiaria(String razonSocial) {
    PersonaJuridica juridica = new PersonaJuridica(
        DatosDeTest.cuitUnico(), razonSocial, TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    entityManager().flush();
    return beneficiaria;
  }

  private static List<String> razonesSociales(PropuestaDTO.RankingDTO ranking) {
    return ranking.candidatas().stream().map(PropuestaDTO.CandidataDTO::razonSocial).toList();
  }

  private static List<Integer> posiciones(PropuestaDTO.RankingDTO ranking) {
    return ranking.candidatas().stream().map(PropuestaDTO.CandidataDTO::posicion).toList();
  }
}
