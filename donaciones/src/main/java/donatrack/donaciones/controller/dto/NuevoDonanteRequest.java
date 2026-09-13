package donatrack.donaciones.controller.dto;

public record NuevoDonanteRequest(
    String email,
    String tipo,
    String direccion,
    // PersonaHumana
    String nombre,
    String apellido,
    Integer edad,
    String documento,
    String genero,
    // PersonaJuridica
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro
) {
}