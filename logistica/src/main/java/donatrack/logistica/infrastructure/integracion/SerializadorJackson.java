package donatrack.logistica.infrastructure.integracion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import donatrack.logistica.domain.integracion.IntegracionExternaException;
import donatrack.logistica.domain.integracion.SerializadorJson;

import java.util.List;

public class SerializadorJackson implements SerializadorJson {

  private final ObjectMapper mapper = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Override
  public String aJson(Object objeto) {
    try {
      return mapper.writeValueAsString(objeto);
    } catch (Exception e) {
      throw new IntegracionExternaException("No se pudo serializar el mensaje a JSON.", e);
    }
  }

  @Override
  public <T> T desdeJson(String json, Class<T> tipo) {
    try {
      return mapper.readValue(json, tipo);
    } catch (Exception e) {
      throw new IntegracionExternaException(
          "No se pudo interpretar la respuesta como " + tipo.getSimpleName() + ".", e
      );
    }
  }

  @Override
  public <T> List<T> listaDesdeJson(String json, Class<T> tipo) {
    try {
      return mapper.readValue(
          json,
          mapper.getTypeFactory().constructCollectionType(List.class, tipo)
      );
    } catch (Exception e) {
      throw new IntegracionExternaException(
          "No se pudo interpretar la respuesta como lista de " + tipo.getSimpleName() + ".", e
      );
    }
  }
}
