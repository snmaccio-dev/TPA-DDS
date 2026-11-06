package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.catalogo.Subcategoria;

import java.util.List;

// El catalogo es de lectura: lo siembra el arranque del microservicio y las pantallas
// que cargan donaciones o necesidades lo necesitan para ofrecer opciones validas en vez
// de que el usuario escriba el nombre a mano.
public record SubcategoriaDTO(
    Long id,
    String nombre,
    String categoria,
    String unidad
) {

  public static SubcategoriaDTO desde(Subcategoria subcategoria) {
    return new SubcategoriaDTO(
        subcategoria.getId(),
        subcategoria.getNombre(),
        subcategoria.getCategoria() == null ? null : subcategoria.getCategoria().getNombre(),
        subcategoria.getUnidades() == null ? null : subcategoria.getUnidades().name()
    );
  }

  public static List<SubcategoriaDTO> desde(List<Subcategoria> subcategorias) {
    return subcategorias.stream().map(SubcategoriaDTO::desde).toList();
  }
}
