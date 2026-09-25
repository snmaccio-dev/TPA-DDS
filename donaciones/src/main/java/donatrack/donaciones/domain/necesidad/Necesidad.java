package donatrack.donaciones.domain.necesidad;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.persona.Beneficiaria;
import javax.persistence.*;

@Entity
@Table(name = "Necesidad")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_de_necesidad")
public abstract class Necesidad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "necesidad_id")
  private Long id;

  @Column(name="Descripcion")
  protected String descripcion;

  @Column(name="cantidad")
  protected int cantidad;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  protected Subcategoria subcategoria;

  @ManyToOne
  @JoinColumn(name = "entidad_id")
  private Beneficiaria entidad;

  protected Necesidad() {
  }

  public Necesidad(
      String descripcion,
      int cantidad,
      Subcategoria subcategoria
  ) {
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.subcategoria = subcategoria;
  }

  public Long getId() {
    return id;
  }

  public int getCantidad() {
    return cantidad;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public Unidades getUnidades() {
    return subcategoria.getUnidades();
  }

  public String getDescripcion() {
    return descripcion;
  }

  public abstract boolean esExtraordinaria();


  // Metodos para el CRUD
  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public void setCantidad(int cantidad) {
    this.cantidad = cantidad;
  }

  public void setSubcategoria(Subcategoria subcategoria) {
    this.subcategoria = subcategoria;
  }
}