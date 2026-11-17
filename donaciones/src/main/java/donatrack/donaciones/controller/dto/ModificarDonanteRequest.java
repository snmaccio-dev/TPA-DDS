package donatrack.donaciones.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import donatrack.donaciones.service.DatosDonante;

// El documento, el CUIT y el email no son editables y no se leen de este body: si vienen,
// se ignoran. El documento o CUIT identifica al donante en la ruta y el email es el
// usuario de su cuenta en Keycloak. Los campos que no corresponden al tipo de persona
// del donante tambien se ignoran.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModificarDonanteRequest(
    String nombre,
    String apellido,
    Integer edad,
    String genero,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    String whatsapp,
    String contactoPredeterminado
) {

  public DatosDonante aDatos() {
    return new DatosDonante(nombre, apellido, edad, genero, razonSocial, tipoOrganizacion,
        rubro, direccion, telefono, whatsapp, contactoPredeterminado);
  }
}
