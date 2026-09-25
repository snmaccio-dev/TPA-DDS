package donatrack.donaciones.service;

import donatrack.donaciones.domain.persona.Representante;

import java.util.List;

public record DatosEntidad(String direccion, String telefono, List<Representante> representantes) {

    public DatosEntidad {
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La direccion de la beneficiaria es obligatoria.");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El telefono de la beneficiaria es obligatorio.");
        }
        if (representantes == null || representantes.isEmpty()) {
            throw new IllegalArgumentException("La beneficiaria debe indicar al menos un representante con su correo.");
        }
        direccion = direccion.trim();
        telefono = telefono.trim();
        representantes = List.copyOf(representantes);
    }
}
