package donatrack.web.vista;

import java.util.List;

// Una pagina de un listado paginado de Donaciones ({items, total, pagina, tamanio,
// totalPaginas}). total es el de todo el filtro, no el de la pagina.
public record PaginaVista<T>(
    List<T> items,
    long total,
    int pagina,
    int tamanio,
    int totalPaginas
) {

  public PaginaVista {
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static <T> PaginaVista<T> vacia() {
    return new PaginaVista<>(List.of(), 0, 1, 0, 0);
  }

  public List<T> getItems() { return items; }
  public long getTotal() { return total; }
  public int getPagina() { return pagina; }
  public boolean isVacia() { return items.isEmpty(); }

  // "Pagina 1 de 1" aunque no haya resultados.
  public int getTotalPaginasTexto() { return Math.max(1, totalPaginas); }

  public boolean isHayAnterior() { return pagina > 1; }
  public boolean isHaySiguiente() { return pagina < totalPaginas; }
}
