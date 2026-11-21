package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.ReglasDePersona;
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

    // Alta de una entidad: el telefono y todos los representantes tienen que cumplir las
    // reglas de ReglasDePersona.
    public void validarParaAlta() {
        ReglasDePersona.telefono("El teléfono", telefono);
        representantes.forEach(ReglasDePersona::representante);
    }

    // Modificacion: solo se valida lo que cambia respecto de la entidad actual. Un telefono
    // o un representante viejo que no cumple las reglas se puede conservar tal cual.
    public void validarCambiosRespectoDe(PersonaJuridica actual) {
        if (!actual.tieneContacto(TipoContacto.TELEFONO, telefono)) {
            ReglasDePersona.telefono("El teléfono", telefono);
        }
        List<Representante> actuales = actual.getRepresentantes();
        representantes.stream()
            .filter(r -> !actuales.contains(r))
            .forEach(ReglasDePersona::representante);
    }

    public void aplicarA(PersonaJuridica juridica) {
        juridica.setDireccion(direccion);
        if (!juridica.tieneContacto(TipoContacto.TELEFONO, telefono)) {
            juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, telefono));
        }
        for (Representante r : representantes) {
            juridica.agregarRepresentante(r);
        }
    }

    public void reemplazarEn(PersonaJuridica juridica) {
        juridica.setDireccion(direccion);
        juridica.reemplazarContacto(TipoContacto.TELEFONO, telefono);
        juridica.reemplazarRepresentantes(representantes);
    }
}