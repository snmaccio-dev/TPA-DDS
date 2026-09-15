package donatrack.logistica.controller.dto;

import java.util.List;

public record ConfirmarRecepcionRequest(List<String> fotos) {

  public List<String> fotosOVacio() {
    return fotos == null ? List.of() : List.copyOf(fotos);
  }
}
