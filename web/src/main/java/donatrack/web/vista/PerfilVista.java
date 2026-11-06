package donatrack.web.vista;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public record PerfilVista(
    String keycloakId,
    String email,
    List<String> roles,
    Long donanteId,
    Long beneficiariaId,
    String nombre
) {

  private static final String ADMINISTRADOR = "ADMINISTRADOR";
  private static final String BENEFICIARIA = "BENEFICIARIA";
  private static final String DONANTE = "DONANTE";
  private static final String CHOFER = "CHOFER";

  public PerfilVista {
    roles = roles == null ? List.of() : List.copyOf(roles);
  }

  public static PerfilVista deDemostracion() {
    return new PerfilVista(null, null, List.of(), null, null, "Usuario de prueba");
  }

  public String getNombre() {
    return nombre == null || nombre.isBlank() ? email : nombre;
  }

  public String getEmail() {
    return email;
  }

  public String getIniciales() {
    String texto = getNombre();

    if (texto == null || texto.isBlank()) {
      return "US";
    }

    String iniciales = Arrays.stream(texto.trim().split("\\s+"))
        .filter(parte -> !parte.isEmpty())
        .limit(2)
        .map(parte -> parte.substring(0, 1))
        .reduce("", String::concat)
        .toUpperCase(Locale.ROOT);

    return iniciales.length() == 1 && texto.length() > 1
        ? texto.substring(0, 2).toUpperCase(Locale.ROOT)
        : iniciales;
  }

  public boolean isAdministrador() {
    return roles.contains(ADMINISTRADOR);
  }

  public boolean isBeneficiaria() {
    return roles.contains(BENEFICIARIA);
  }

  public boolean isDonante() {
    return roles.contains(DONANTE);
  }

  public boolean isChofer() {
    return roles.contains(CHOFER);
  }

  public String inicioSegunRol() {
    if (isAdministrador()) {
      return "/admin-dashboard";
    }
    if (isBeneficiaria()) {
      return "/beneficiario-dashboard";
    }
    if (isDonante()) {
      return "/donante-dashboard";
    }

    return "/";
  }
}
