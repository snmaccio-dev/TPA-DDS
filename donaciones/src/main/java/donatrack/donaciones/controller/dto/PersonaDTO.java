package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;

import java.util.List;

public record PersonaDTO(
    String tipo,
    String documento,
    String nombreDisplay,
    String direccion,
    List<ContactoDTO> contactos
) {

  public record ContactoDTO(String tipo, String valor) {}

  public static PersonaDTO desde(Persona persona) {
    return new PersonaDTO(
        tipoDe(persona),
        persona.getDocumento(),
        persona.getNombreDisplay(),
        persona.getDireccion(),
        persona.getContactos().stream()
            .map(PersonaDTO::contactoDTO)
            .toList()
    );
  }

  public static List<PersonaDTO> desde(List<Persona> personas) {
    return personas.stream().map(PersonaDTO::desde).toList();
  }

  private static String tipoDe(Persona persona) {
    if (persona instanceof PersonaHumana) return "HUMANA";
    if (persona instanceof PersonaJuridica) return "JURIDICA";
    return persona.getClass().getSimpleName();
  }

  private static ContactoDTO contactoDTO(MedioContacto medio) {
    return new ContactoDTO(medio.getTipo().name(), medio.getValor());
  }
}