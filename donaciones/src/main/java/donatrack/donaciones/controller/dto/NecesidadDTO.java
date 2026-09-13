package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.necesidad.Necesidad;

import java.util.List;

public record NecesidadDTO(
    long id,
    String descripcion,
    int cantidad,
    String subcategoria,
    String unidad,
    boolean esExtraordinaria
) {

  public static NecesidadDTO desde(Necesidad necesidad) {
    return new NecesidadDTO(
        necesidad.getId(),
        necesidad.getDescripcion(),
        necesidad.getCantidad(),
        necesidad.getSubcategoria().getNombre(),
        necesidad.getUnidades().name(),
        necesidad.esExtraordinaria()
    );
  }

  public static List<NecesidadDTO> desde(List<Necesidad> necesidades) {
    return necesidades.stream().map(NecesidadDTO::desde).toList();
  }
}