package donatrack.donaciones.controller.dto;

import java.util.List;

// Registro publico de una beneficiaria: los campos de persona juridica mas
// email y contrasena. toString oculta la contrasena.
public record RegistroBeneficiariaRequest(
    String email,
    String contrasena,
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    List<RepresentanteRequest> representantes
) {

    @Override
    public String toString() {
        return "RegistroBeneficiariaRequest{email=" + email + ", cuit=" + cuit
            + ", razonSocial=" + razonSocial + ", contrasena=***}";
    }
}