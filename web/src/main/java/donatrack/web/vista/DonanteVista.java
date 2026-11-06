package donatrack.web.vista;

import java.util.List;

public record DonanteVista(
    String tipo,
    String documento,
    String nombreDisplay,
    String direccion,
    List<Contacto> contactos
) {

  public record Contacto(String tipo, String valor) {
    public String getTipo() { return tipo; }
    public String getValor() { return valor; }
  }

  public DonanteVista {
    contactos = contactos == null ? List.of() : List.copyOf(contactos);
  }

  public String getTipo() { return tipo; }
  public String getDocumento() { return documento; }
  public String getNombreDisplay() { return nombreDisplay; }
  public String getDireccion() { return direccion; }
  public List<Contacto> getContactos() { return contactos; }

  public String getTipoTexto() {
    if ("HUMANA".equalsIgnoreCase(tipo)) return "Persona humana";
    if ("JURIDICA".equalsIgnoreCase(tipo)) return "Persona jurídica";
    return tipo;
  }

  public String getEmail() {
    if (contactos == null) return "";
    return contactos.stream()
        .filter(c -> c.tipo() != null && c.tipo().toUpperCase().contains("MAIL"))
        .map(Contacto::valor)
        .findFirst()
        .orElse("");
  }
}
