package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;

import java.util.List;

public record DatosEntidad(String direccion, String telefono, List<Representante> representantes) {

    public DatosEntidad {
        if (direccion == null || direccion.isBlank()) {
            throw new DatosInvalidosException("La direccion de la beneficiaria es obligatoria.");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new DatosInvalidosException("El telefono de la beneficiaria es obligatorio.");
        }
        if (representantes == null || representantes.isEmpty()) {
            throw new DatosInvalidosException("La beneficiaria debe indicar al menos un representante con su correo.");
        }
        direccion = direccion.trim();
        telefono = telefono.trim();
        representantes = List.copyOf(representantes);
    }

    // Aplica los datos sobre la persona juridica. Centralizado aca para que
    // GestorEntidadesBeneficiarias y GestorRegistro no dupliquen la logica.
    public void aplicarA(PersonaJuridica juridica) {
        juridica.setDireccion(direccion);
        if (!juridica.tieneContacto(TipoContacto.TELEFONO, telefono)) {
            juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, telefono));
        }
        for (Representante r : representantes) {
            juridica.agregarRepresentante(r);
        }
    }
}