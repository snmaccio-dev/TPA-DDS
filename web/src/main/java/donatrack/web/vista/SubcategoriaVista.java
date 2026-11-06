package donatrack.web.vista;

public record SubcategoriaVista(
    Long id,
    String nombre,
    String categoria,
    String unidad
) {

  public String getNombre() {
    return nombre;
  }

  public String getCategoria() {
    return categoria;
  }

  // En el select conviene ver la categoria para distinguir, por ejemplo, "Sillas" de
  // mobiliario frente a una subcategoria de nombre parecido en otra categoria.
  public String getEtiqueta() {
    return categoria == null || categoria.isBlank()
        ? nombre
        : nombre + " (" + categoria + ")";
  }
}
