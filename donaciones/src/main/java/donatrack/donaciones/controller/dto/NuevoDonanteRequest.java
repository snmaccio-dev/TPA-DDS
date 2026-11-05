package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;

public record NuevoDonanteRequest(
    String email,
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

    public Persona aPersona() {
        if ("HUMANA".equalsIgnoreCase(tipo)) {
            return new PersonaHumana(
                nombre,
                apellido,
                edad == null ? 0 : edad,
                documento,
                genero == null ? null : Genero.valueOf(genero)
            );
        }
        if ("JURIDICA".equalsIgnoreCase(tipo)) {
            return new PersonaJuridica(
                cuit,
                razonSocial,
                tipoOrganizacion == null ? null : TipoOrganizacion.valueOf(tipoOrganizacion),
                rubro
            );
        }
        throw new DatosInvalidosException(
            "El tipo de persona debe ser HUMANA o JURIDICA (recibido: " + tipo + ")."
        );
    }
}