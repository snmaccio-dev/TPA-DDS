package donatrack.logistica.graphql;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.Map;

public record PedidoGraphQl(
    String query,
    String operationName,
    Map<String, Object> variables
) {

  public String consultaObligatoria() {
    if (query == null || query.isBlank()) {
      throw new DatosInvalidosException("El pedido debe incluir una consulta GraphQL.");
    }

    return query;
  }

  public Map<String, Object> variablesOVacio() {
    return variables == null ? Map.of() : Map.copyOf(variables);
  }
}
