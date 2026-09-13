package donatrack.donaciones.controller.dto;

public record NuevaBeneficiariaExistenteRequest(
    String cuit,
    String direccion,
    String telefono
) {
}