package donatrack.web.integracion;

// Espeja el RegistroDonanteRequest de Donaciones. El tipo queda fijo en HUMANA: el
// formulario publico da de alta personas, y el alta de donantes juridicos pasa por la
// pantalla de administracion.
public record RegistroDonante(
    String email,
    String contrasena,
    String tipo,
    String direccion,
    String nombre,
    String apellido,
    Integer edad,
    String documento,
    String genero
) {

  public static RegistroDonante humana(String email,
                                       String contrasena,
                                       String direccion,
                                       String nombre,
                                       String apellido,
                                       Integer edad,
                                       String documento,
                                       String genero) {
    return new RegistroDonante(
        email, contrasena, "HUMANA", direccion, nombre, apellido, edad, documento, genero);
  }

  @Override
  public String toString() {
    return "RegistroDonante{email=" + email + ", documento=" + documento + ", contrasena=***}";
  }
}
