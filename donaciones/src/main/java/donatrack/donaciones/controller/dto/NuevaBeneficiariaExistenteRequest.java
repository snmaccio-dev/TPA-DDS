package donatrack.donaciones.controller.dto;

import java.util.List;

public record NuevaBeneficiariaExistenteRequest(
    String cuit,
    String direccion,
    String telefono,
    List<RepresentanteRequest> representantes
) {
}
