package donatrack.logistica.domain.integracion;

import java.util.List;

public interface SerializadorJson {

  String aJson(Object objeto);

  <T> T desdeJson(String json, Class<T> tipo);

  <T> List<T> listaDesdeJson(String json, Class<T> tipo);
}
