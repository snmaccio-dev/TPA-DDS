package donatrack.web.integracion;

import java.util.List;

public record FotosSubidas(List<String> urls) {

  public List<String> urlsOVacio() {
    return urls == null ? List.of() : List.copyOf(urls);
  }
}
