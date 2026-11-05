package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.persona.Persona;

// Registro publico desde el web: usa los mismos campos de perfil que
// NuevoDonanteRequest y agrega la contrasena. toString() oculta la contrasena
// para que no aparezca en logs ni mensajes de excepcion.
public record RegistroDonanteRequest(
    String email,
    String contrasena,
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

    public NuevoDonanteRequest comoNuevoDonante() {
        return new NuevoDonanteRequest(
            email, tipo, direccion,
            nombre, apellido, edad, documento, genero,
            cuit, razonSocial, tipoOrganizacion, rubro
        );
    }

    public Persona aPersona() {
        return comoNuevoDonante().aPersona();
    }

    @Override
    public String toString() {
        return "RegistroDonanteRequest{email=" + email + ", documento=" + documento
            + ", cuit=" + cuit + ", contrasena=***}";
    }
}