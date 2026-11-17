package donatrack.donaciones.infrastructure.importacion;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;

public final class PersonaFactory {

    private PersonaFactory() {
    }

    public static Persona crear(FilaDonanteCsv fila) {
        Persona persona = switch (fila.tipoPersona()) {
            case HUMANA -> crearPersonaHumana(fila);
            case JURIDICA -> new PersonaJuridica(
                fila.documento(), fila.nombre(), TipoOrganizacion.EMPRESA, "");
        };
        persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, fila.email()));
        if (fila.tieneTelefono()) {
            persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, fila.telefono()));
        }
        return persona;
    }

    public static NombreYApellido separarNombre(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+", 2);
        return new NombreYApellido(partes[0], partes.length > 1 ? partes[1] : "");
    }

    private static PersonaHumana crearPersonaHumana(FilaDonanteCsv fila) {
        NombreYApellido nombre = separarNombre(fila.nombre());
        return new PersonaHumana(
            nombre.nombre(), nombre.apellido(), 0, fila.documento(), Genero.OTRO);
    }

    public record NombreYApellido(String nombre, String apellido) {
    }
}
