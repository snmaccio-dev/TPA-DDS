package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.asignacion.CandidataPropuesta;
import donatrack.donaciones.domain.donacion.asignacion.OrigenCandidata;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.List;

// rankings: si hubo coincidencias, una sola lista (AMBOS); si no, una por algoritmo. Cada
// una trae sus candidatas en orden de ranking. candidatas: todas las entidades propuestas,
// sin repetir, para quien solo necesita la lista plana.
public record PropuestaDTO(
    long donacionId,
    String fechaGeneracion,
    boolean huboCoincidencias,
    List<RankingDTO> rankings,
    List<CandidataDTO> candidatas
) {

  public record RankingDTO(String algoritmo, String nombre, List<CandidataDTO> candidatas) {}

  public record CandidataDTO(long id, String razonSocial, String cuit, Integer posicion) {}

  public static PropuestaDTO desde(PropuestaAsignacion propuesta) {
    List<RankingDTO> rankings = propuesta.getOrigenes().stream()
        .map(origen -> rankingDe(propuesta, origen))
        .toList();

    return new PropuestaDTO(
        propuesta.getDonacionId(),
        Fechas.aTexto(propuesta.getFechaGeneracion()),
        propuesta.huboCoincidencias(),
        rankings,
        rankings.stream().flatMap(ranking -> ranking.candidatas().stream()).toList()
    );
  }

  public static List<PropuestaDTO> desde(List<PropuestaAsignacion> propuestas) {
    return propuestas.stream().map(PropuestaDTO::desde).toList();
  }

  private static RankingDTO rankingDe(PropuestaAsignacion propuesta, OrigenCandidata origen) {
    return new RankingDTO(
        origen.name(),
        origen.getNombre(),
        propuesta.candidatasDe(origen).stream().map(PropuestaDTO::candidataDe).toList()
    );
  }

  private static CandidataDTO candidataDe(CandidataPropuesta candidata) {
    Beneficiaria beneficiaria = candidata.getBeneficiaria();
    return new CandidataDTO(
        beneficiaria.getId(),
        beneficiaria.getPersona().getRazonSocial(),
        beneficiaria.getPersona().getCuit(),
        candidata.getPosicion()
    );
  }
}
