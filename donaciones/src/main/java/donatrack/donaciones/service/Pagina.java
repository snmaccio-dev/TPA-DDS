package donatrack.donaciones.service;

import java.util.List;

// Una pagina de un listado: los elementos, el total que cumple el filtro y la pagina
// pedida (desde 1) con su tamanio.
public record Pagina<T>(List<T> items, long total, int pagina, int tamanio) {

  public Pagina {
    items = List.copyOf(items);
  }

  public int totalPaginas() {
    return total == 0 ? 0 : (int) ((total + tamanio - 1) / tamanio);
  }
}
