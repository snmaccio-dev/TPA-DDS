package donatrack.web.integracion;

// Cuerpo del POST /donantes (alta desde el panel de administracion).
// Los nombres coinciden con NuevoDonanteRequest del modulo donaciones para que Jackson
// los lea sin alias.
public record NuevoDonanteAdmin(
    String email,
    String tipo,
    String direccion,
    String nombre,
    String apellido,
    Integer edad,
    String documento,
    String genero,
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro
) {}
