package donatrack.web.vista;

import java.util.List;

public record EntidadVista(
    Long id,
    String cuit,
    String razonSocial,
    String direccion,
    String telefono,
    String tipoOrganizacion,
    String rubro,
    List<RepresentanteVista> representantes
) {

  public record RepresentanteVista(String nombre, String apellido, String email) {
    public String getNombre() {
      return nombre;
    }

    public String getApellido() {
      return apellido;
    }

    public String getEmail() {
      return email;
    }
  }

  public EntidadVista {
    representantes = representantes == null ? List.of() : List.copyOf(representantes);
  }

  public Long getId() {
    return id;
  }

  public String getCuit() {
    return cuit;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getRubro() {
    return rubro;
  }

  // El enum viaja en mayusculas desde Donaciones. En pantalla queda mejor capitalizado.
  public String getTipoTexto() {
    if (tipoOrganizacion == null || tipoOrganizacion.isBlank()) {
      return "Sin clasificar";
    }

    return tipoOrganizacion.charAt(0) + tipoOrganizacion.substring(1).toLowerCase();
  }

  public List<RepresentanteVista> getRepresentantes() {
    return representantes;
  }

  public String getInicial() {
    return razonSocial == null || razonSocial.isBlank()
        ? "?"
        : razonSocial.substring(0, 1).toUpperCase();
  }
}
