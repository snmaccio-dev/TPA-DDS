package donatrack.logistica.domain.ruta;

import donatrack.logistica.domain.entrega.Entrega;

import java.util.ArrayList;
import java.util.List;

public class DestinoEntrega {

  private final String direccion;
  private final List<Entrega> entregas;

  public DestinoEntrega(String direccion, List<Entrega> entregas) {
    if (direccion == null || direccion.isBlank()) {
      throw new IllegalArgumentException("El destino debe tener una direccion.");
    }
    if (entregas == null || entregas.isEmpty()) {
      throw new IllegalArgumentException("El destino debe tener al menos una entrega.");
    }
    this.direccion = direccion;
    this.entregas = new ArrayList<>(entregas);
  }

  public String getDireccion() {
    return direccion;
  }

  public List<Entrega> getEntregas() {
    return new ArrayList<>(entregas);
  }
}
