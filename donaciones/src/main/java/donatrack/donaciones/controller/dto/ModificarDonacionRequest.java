package donatrack.donaciones.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import donatrack.donaciones.domain.donacion.DatosModificacionBien;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModificarDonacionRequest(
    String descripcion,
    String fotoUrl,
    List<BienModificado> bienes
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record BienModificado(Long id, String descripcion, Double cantidad) {}

  public List<DatosModificacionBien> datosBienes() {
    if (bienes == null) {
      return List.of();
    }
    return bienes.stream()
        .map(b -> b == null ? null : new DatosModificacionBien(b.id(), b.descripcion(), b.cantidad()))
        .toList();
  }
}
