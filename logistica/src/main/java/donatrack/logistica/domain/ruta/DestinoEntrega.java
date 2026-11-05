package donatrack.logistica.domain.ruta;

import donatrack.logistica.domain.entrega.Entrega;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "destino_entrega")
public class DestinoEntrega {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private int orden;

  @Column(nullable = false)
  private String direccion;

  @OneToMany(mappedBy = "destino")
  private List<Entrega> entregas;

  protected DestinoEntrega() {
  }

  public DestinoEntrega(int orden, String direccion, List<Entrega> entregas) {
    if (orden < 0) {
      throw new IllegalArgumentException("El orden del destino no puede ser negativo.");
    }
    if (direccion == null || direccion.isBlank()) {
      throw new IllegalArgumentException("El destino debe tener una direccion.");
    }
    if (entregas == null || entregas.isEmpty()) {
      throw new IllegalArgumentException("El destino debe tener al menos una entrega.");
    }
    this.orden = orden;
    this.direccion = direccion;
    this.entregas = new ArrayList<>(entregas);
    this.entregas.forEach(entrega -> entrega.asignarADestino(this));
  }

  public Long getId() {
    return id;
  }

  public int getOrden() {
    return orden;
  }

  public String getDireccion() {
    return direccion;
  }

  public List<Entrega> getEntregas() {
    return new ArrayList<>(entregas);
  }
}
