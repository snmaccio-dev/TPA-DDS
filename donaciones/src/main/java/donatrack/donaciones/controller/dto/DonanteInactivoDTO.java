package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.persona.Donante;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record DonanteInactivoDTO(PersonaDTO persona, String ultimaInteraccion) {

  public static DonanteInactivoDTO desde(Donante donante) {
    return new DonanteInactivoDTO(
        PersonaDTO.desde(donante.getPersona()),
        donante.getUltimaInteraccion() == null
            ? null
            : donante.getUltimaInteraccion().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );
  }

  public static List<DonanteInactivoDTO> desde(List<Donante> donantes) {
    return donantes.stream().map(DonanteInactivoDTO::desde).toList();
  }
}