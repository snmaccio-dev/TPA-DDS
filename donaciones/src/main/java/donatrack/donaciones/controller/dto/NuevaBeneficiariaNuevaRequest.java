package donatrack.donaciones.controller.dto;

public record NuevaBeneficiariaNuevaRequest(
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono
) {
}