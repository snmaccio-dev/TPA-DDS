package donatrack.logistica.domain.ruta;

import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.entrega.Entrega;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class DestinoEntrega {

  @Column(nullable = false)
  private int orden;

  @Column(nullable = false)
  private String direccion;

  @Transient
  private final List<Entrega> entregasIniciales = new ArrayList<>();

  protected DestinoEntrega() {
  }

  public DestinoEntrega(int orden, String direccion, List<Entrega> entregas) {
    if (orden < 0) {
      throw new DatosInvalidosException("El orden del destino no puede ser negativo.");
    }
    if (direccion == null || direccion.isBlank()) {
      throw new DatosInvalidosException("El destino debe tener una direccion.");
    }
    if (entregas == null || entregas.isEmpty()) {
      throw new DatosInvalidosException("El destino debe tener al menos una entrega.");
    }
    this.orden = orden;
    this.direccion = direccion;
    this.entregasIniciales.addAll(entregas);
    this.entregasIniciales.forEach(entrega -> entrega.asignarADestino(orden));
  }

  List<Entrega> getEntregasIniciales() {
    return new ArrayList<>(entregasIniciales);
  }

  public int getOrden() {
    return orden;
  }

  public String getDireccion() {
    return direccion;
  }
}
