package donatrack.web.sesion;

import io.javalin.http.Context;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class IdentidadPorQueryString implements IdentidadDelUsuario {

  private static final String COMO_IDENTIFICARSE =
      "Todavía no hay inicio de sesión: agregá el parámetro correspondiente a la URL "
          + "para indicar de quién querés ver los datos.";

  @Override
  public Optional<Long> beneficiaria(Context ctx) {
    return numero(ctx, "beneficiaria");
  }

  @Override
  public Optional<List<Long>> donaciones(Context ctx) {
    String valores = ctx.queryParam("donaciones");

    if (valores == null || valores.isBlank()) {
      return Optional.empty();
    }

    List<Long> ids = Arrays.stream(valores.split(","))
        .map(String::trim)
        .filter(valor -> !valor.isEmpty())
        .map(IdentidadPorQueryString::comoNumero)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    return ids.isEmpty() ? Optional.empty() : Optional.of(ids);
  }

  @Override
  public Optional<Long> entregaAConfirmar(Context ctx) {
    return numero(ctx, "entrega");
  }

  @Override
  public String comoIdentificarse() {
    return COMO_IDENTIFICARSE;
  }

  private Optional<Long> numero(Context ctx, String parametro) {
    String valor = ctx.queryParam(parametro);
    return valor == null || valor.isBlank() ? Optional.empty() : comoNumero(valor);
  }

  private static Optional<Long> comoNumero(String valor) {
    try {
      return Optional.of(Long.parseLong(valor.trim()));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
