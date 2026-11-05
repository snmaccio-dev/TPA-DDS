package donatrack.donaciones.service;

import java.util.List;

public record ResultadoCorrida(int exitosas, List<String> fallidas) {

  public ResultadoCorrida {
    fallidas = fallidas == null ? List.of() : List.copyOf(fallidas);
  }

  public int cantidadDeFallas() {
    return fallidas.size();
  }
}
