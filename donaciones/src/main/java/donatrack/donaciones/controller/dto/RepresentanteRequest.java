package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.persona.Representante;

public record RepresentanteRequest(String nombre, String apellido, String email) {

    public Representante aRepresentante() {
        return new Representante(nombre, apellido, email);
    }
}
