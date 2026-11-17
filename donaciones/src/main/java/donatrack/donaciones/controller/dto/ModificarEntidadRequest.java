package donatrack.donaciones.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModificarEntidadRequest(
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    List<RepresentanteRequest> representantes
) {
}
