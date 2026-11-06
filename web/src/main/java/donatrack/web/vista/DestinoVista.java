package donatrack.web.vista;

import java.util.List;

public record DestinoVista(
    String direccion,
    List<EntregaVista> entregas
) {

  public DestinoVista {
    entregas = entregas == null ? List.of() : List.copyOf(entregas);
  }

  public String getDireccion() {
    return direccion;
  }

  public List<EntregaVista> getEntregas() {
    return entregas;
  }

  public int getCantidadDeEntregas() {
    return entregas.size();
  }
}
