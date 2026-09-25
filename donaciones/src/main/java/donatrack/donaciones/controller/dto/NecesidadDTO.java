package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.necesidad.Necesidad;

import java.util.List;

public record NecesidadDTO(
    Long id,
    String descripcion,
    int cantidad,
    String subcategoria,
    String unidad,
    boolean esExtraordinaria,
    String cuitBeneficiaria
) {

  public static NecesidadDTO desde(Necesidad necesidad) {
    return new NecesidadDTO(
        necesidad.getId(),
        necesidad.getDescripcion(),
        necesidad.getCantidad(),
        necesidad.getSubcategoria().getNombre(),
        necesidad.getUnidades().name(),
        necesidad.esExtraordinaria(),
        necesidad.getEntidad().getPersona().getCuit()
    );
  }

  public static List<NecesidadDTO> desde(List<Necesidad> necesidades) {
    return necesidades.stream().map(NecesidadDTO::desde).toList();
  }
}
