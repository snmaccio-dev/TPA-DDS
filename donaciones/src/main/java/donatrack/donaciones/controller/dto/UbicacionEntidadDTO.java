package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Coordenadas;

import java.util.List;

// Lo unico que expone el endpoint publico de ubicaciones: sin contactos, representantes
// ni datos de personas.
public record UbicacionEntidadDTO(long id, String razonSocial, double latitud, double longitud) {

  public static List<UbicacionEntidadDTO> desde(List<Beneficiaria> beneficiarias) {
    return beneficiarias.stream()
        .filter(Beneficiaria::tieneUbicacion)
        .map(b -> {
          Coordenadas ubicacion = b.getUbicacion().orElseThrow();
          return new UbicacionEntidadDTO(
              b.getId(),
              b.getPersona().getRazonSocial(),
              ubicacion.getLatitud(),
              ubicacion.getLongitud());
        })
        .toList();
  }
}
