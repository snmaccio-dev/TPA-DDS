package donatrack.web.integracion;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Arma la query string para GET /donaciones. La API acepta estado, categoria y
// subcategoria (repetible).
public record FiltroDonacionesWeb(
    String estado,
    String categoria,
    List<String> subcategorias
) {

  public FiltroDonacionesWeb {
    subcategorias = subcategorias == null ? List.of() : List.copyOf(subcategorias);
  }

  public static FiltroDonacionesWeb vacio() {
    return new FiltroDonacionesWeb(null, null, List.of());
  }

  public String aQueryString() {
    List<String> partes = new ArrayList<>();
    agregar(partes, "estado", estado);
    agregar(partes, "categoria", categoria);
    for (String sub : subcategorias) {
      agregar(partes, "subcategoria", sub);
    }
    return partes.isEmpty() ? "" : "?" + String.join("&", partes);
  }

  private static void agregar(List<String> partes, String clave, String valor) {
    if (valor == null || valor.isBlank()) return;
    partes.add(clave + "=" + URLEncoder.encode(valor, StandardCharsets.UTF_8));
  }
}
