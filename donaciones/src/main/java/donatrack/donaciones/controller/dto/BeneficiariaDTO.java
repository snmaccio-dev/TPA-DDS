package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;

import java.util.List;

public record BeneficiariaDTO(
    long id,
    String cuit,
    String razonSocial,
    String direccion,
    String telefono,
    String tipoOrganizacion,
    String rubro
) {

  public static BeneficiariaDTO desde(Beneficiaria beneficiaria) {
    PersonaJuridica juridica = beneficiaria.getPersona();
    return new BeneficiariaDTO(
        beneficiaria.getId(),
        juridica.getCuit(),
        juridica.getRazonSocial(),
        juridica.getDireccion(),
        primerContactoDeTipo(juridica, TipoContacto.TELEFONO),
        juridica.getTipo() == null ? null : juridica.getTipo().name(),
        juridica.getRubro()
    );
  }

  public static List<BeneficiariaDTO> desde(List<Beneficiaria> beneficiarias) {
    return beneficiarias.stream().map(BeneficiariaDTO::desde).toList();
  }

  private static String primerContactoDeTipo(PersonaJuridica juridica, TipoContacto tipo) {
    return juridica.getContactos().stream()
        .filter(c -> c.getTipo() == tipo)
        .map(MedioContacto::getValor)
        .findFirst()
        .orElse(null);
  }
}