package donatrack.logistica.graphql;

import donatrack.logistica.seguridad.RolDeAcceso;
import graphql.ExecutionInput;
import graphql.GraphQL;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Objects;

public class GraphQlController {

  private final GraphQL graphQl;

  public GraphQlController(GraphQL graphQl) {
    Objects.requireNonNull(graphQl, "Debe indicarse el motor GraphQL.");
    this.graphQl = graphQl;
  }

  // Un solo endpoint para todas las consultas: los roles por ruta ya no alcanzan para
  // distinguir que puede ver cada uno. Por eso hoy queda restringido a ADMINISTRADOR, y
  // abrirlo a otros roles exige chequear la propiedad dentro de cada data fetcher.
  public void registrarRutas(Javalin app) {
    app.post("/graphql", this::ejecutar, RolDeAcceso.ADMINISTRADOR);
  }

  private void ejecutar(Context ctx) {
    PedidoGraphQl pedido = ctx.bodyAsClass(PedidoGraphQl.class);

    ExecutionInput entrada = ExecutionInput.newExecutionInput()
        .query(pedido.consultaObligatoria())
        .operationName(pedido.operationName())
        .variables(pedido.variablesOVacio())
        .build();

    ctx.json(graphQl.execute(entrada).toSpecification());
  }
}
