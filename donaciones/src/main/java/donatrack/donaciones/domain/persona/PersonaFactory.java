package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;

// Factory Method — crea PersonaHumana o PersonaJuridica segun el tipo del CSV
// Formato CSV: TipoPersona | TipoDoc | Documento | Nombre/RazonSocial | Email | Telefono
public class PersonaFactory {

    public static Persona crear(String[] campos) {
        if (campos.length < 6) {
            throw new IllegalArgumentException("Fila CSV con campos insuficientes: " + campos.length);
        }
        String tipo = campos[0].trim().toUpperCase();
        return switch (tipo) {
            case "HUMANA"   -> crearPersonaHumana(campos);
            case "JURIDICA" -> crearPersonaJuridica(campos);
            default -> throw new IllegalArgumentException("TipoPersona desconocido: " + tipo);
        };
    }

    public static String extraerEmail(String[] campos) {
        if (campos.length < 6) {
            throw new IllegalArgumentException("Fila CSV con campos insuficientes: " + campos.length);
        }
        return campos[4].trim();
    }

    private static PersonaHumana crearPersonaHumana(String[] campos) {
        String documento = campos[2].trim();
        String nombre    = campos[3].trim();
        String email     = campos[4].trim();
        String telefono  = campos[5].trim();

        String[] partes = nombre.split(" ", 2);
        String primerNombre = partes[0];
        String apellido     = partes.length > 1 ? partes[1] : "";

        PersonaHumana persona = new PersonaHumana(primerNombre, apellido, 0, documento, Genero.OTRO);
        persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
        if (!telefono.isBlank()) {
            persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, telefono));
        }
        return persona;
    }

    private static PersonaJuridica crearPersonaJuridica(String[] campos) {
        String documento   = campos[2].trim();
        String razonSocial = campos[3].trim();
        String email       = campos[4].trim();
        String telefono    = campos[5].trim();

        PersonaJuridica persona = new PersonaJuridica(razonSocial, TipoOrganizacion.EMPRESA, "");
        persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
        if (!telefono.isBlank()) {
            persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, telefono));
        }
        return persona;
    }
}
