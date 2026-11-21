package donatrack.donaciones.controller.dto;

import donatrack.donaciones.service.Pagina;

import java.util.List;
import java.util.function.Function;

public record PaginaDTO<T>(
    List<T> items,
    long total,
    int pagina,
    int tamanio,
    int totalPaginas
) {

  public static <E, T> PaginaDTO<T> desde(Pagina<E> pagina, Function<E, T> mapeo) {
    return new PaginaDTO<>(
        pagina.items().stream().map(mapeo).toList(),
        pagina.total(),
        pagina.pagina(),
        pagina.tamanio(),
        pagina.totalPaginas()
    );
  }
}
