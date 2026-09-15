package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;

import java.util.List;

public record PropuestaDTO(
    long donacionId,
    String fechaGeneracion,
    boolean huboCoincidencias,
    List<CandidataDTO> candidatas
) {

  public record CandidataDTO(long id, String razonSocial) {}

  public static PropuestaDTO desde(PropuestaAsignacion propuesta) {
    return new PropuestaDTO(
        propuesta.getDonacionId(),
        Fechas.aTexto(propuesta.getFechaGeneracion()),
        propuesta.huboCoincidencias(),
        propuesta.getCandidatas().stream()
            .map(b -> new CandidataDTO(b.getId(), b.getPersona().getRazonSocial()))
            .toList()
    );
  }

  public static List<PropuestaDTO> desde(List<PropuestaAsignacion> propuestas) {
    return propuestas.stream().map(PropuestaDTO::desde).toList();
  }
}