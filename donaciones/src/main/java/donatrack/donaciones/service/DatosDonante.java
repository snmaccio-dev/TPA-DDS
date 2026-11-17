package donatrack.donaciones.service;

public record DatosDonante(
    String nombre,
    String apellido,
    Integer edad,
    String genero,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    String whatsapp,
    String contactoPredeterminado
) {
}
