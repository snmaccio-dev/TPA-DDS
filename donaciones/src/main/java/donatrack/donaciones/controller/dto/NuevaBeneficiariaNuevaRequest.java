package donatrack.donaciones.controller.dto;

import java.util.List;

public record NuevaBeneficiariaNuevaRequest(
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    List<RepresentanteRequest> representantes
) {
}
