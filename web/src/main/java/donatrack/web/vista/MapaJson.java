package donatrack.web.vista;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class MapaJson {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SIN_PUNTOS = "[]";

  private MapaJson() {
  }

  public static String de(List<PuntoDeMapa> puntos) {
    try {
      return JSON.writeValueAsString(puntos).replace("</", "<\\/");
    } catch (JsonProcessingException e) {
      return SIN_PUNTOS;
    }
  }
}
