package donatrack.donaciones.controller.dto;

import java.util.List;

public record NuevaBeneficiariaExistenteRequest(
    String cuit,
    String direccion,
    String telefono,
    List<RepresentanteRequest> representantes,
    // Opcionales: si vienen las dos, la entidad queda ubicada y no se geocodifica.
    Double latitud,
    Double longitud
) {
}
