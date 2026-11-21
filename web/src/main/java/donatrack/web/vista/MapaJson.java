package donatrack.web.vista;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class MapaJson {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SIN_PUNTOS = "[]";

  private MapaJson() {
  }

  // Va dentro de un <script type="application/json">: se escapan los "<" para que ningun
  // texto (una descripcion, una razon social) pueda cerrar el script. Sigue siendo JSON
  // valido.
  public static String de(List<?> puntos) {
    try {
      return JSON.writeValueAsString(puntos).replace("<", "\\u003c");
    } catch (JsonProcessingException e) {
      return SIN_PUNTOS;
    }
  }
}
