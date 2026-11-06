package donatrack.logistica.graphql;

import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.controller.dto.RutaDTO;
import donatrack.logistica.service.GestorLogistica;
import donatrack.logistica.service.GestorRutas;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class EsquemaGraphQl {

  private static final String RUTA_DEL_ESQUEMA = "/graphql/schema.graphqls";

  private EsquemaGraphQl() {
  }

  public static GraphQL construir(GestorRutas gestorRutas, GestorLogistica gestorLogistica) {
    TypeDefinitionRegistry registro = new SchemaParser().parse(leerEsquema());

    // Cada campo de Planificacion resuelve por separado: si la consulta pide solo las
    // rutas, la cola de planificacion ni se consulta. Esa es la ganancia concreta sobre
    // los dos endpoints REST que la pantalla usaba antes.
    RuntimeWiring cableado = RuntimeWiring.newRuntimeWiring()
        .type("Query", tipo -> tipo
            .dataFetcher("planificacion", entorno -> new Planificacion()))
        .type("Planificacion", tipo -> tipo
            .dataFetcher("rutas", entorno -> RutaDTO.desde(gestorRutas.todas()))
            .dataFetcher("pendientes",
                entorno -> EntregaDTO.desde(gestorLogistica.entregasPendientes())))
        .build();

    return GraphQL
        .newGraphQL(new SchemaGenerator().makeExecutableSchema(registro, cableado))
        .build();
  }

  private static String leerEsquema() {
    try (InputStream esquema = EsquemaGraphQl.class.getResourceAsStream(RUTA_DEL_ESQUEMA)) {
      if (esquema == null) {
        throw new IllegalStateException("No se encontro el esquema en " + RUTA_DEL_ESQUEMA + ".");
      }

      return new String(esquema.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo leer el esquema GraphQL.", e);
    }
  }
}
