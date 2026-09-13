package donatrack.donaciones.service;

public record DatosEntidad(String direccion, String telefono) {

    public DatosEntidad {
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La direccion de la beneficiaria es obligatoria.");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El telefono de la beneficiaria es obligatorio.");
        }
        direccion = direccion.trim();
        telefono = telefono.trim();
    }
}