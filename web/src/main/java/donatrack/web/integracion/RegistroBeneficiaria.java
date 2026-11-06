package donatrack.web.integracion;

import java.util.List;

// Espeja el RegistroBeneficiariaRequest de Donaciones.
public record RegistroBeneficiaria(
    String email,
    String contrasena,
    String cuit,
    String razonSocial,
    String tipoOrganizacion,
    String rubro,
    String direccion,
    String telefono,
    List<Representante> representantes
) {

  public record Representante(String nombre, String apellido, String email) {
  }

  @Override
  public String toString() {
    return "RegistroBeneficiaria{email=" + email + ", cuit=" + cuit + ", contrasena=***}";
  }
}
