package donatrack.logistica.domain.flota;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "camion")
public class Camion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String patente;

  @Column(name = "capacidad_volumen", nullable = false)
  private double capacidadVolumen;

  @Column(nullable = false)
  private double altura;

  @Column(name = "capacidad_carga", nullable = false)
  private double capacidadCarga;

  protected Camion() {
  }

  public Camion(String patente,
                double capacidadVolumen,
                double altura,
                double capacidadCarga) {
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.altura = altura;
    this.capacidadCarga = capacidadCarga;
  }

  public Long getId() {
    return id;
  }

  public String getPatente() {
    return patente;
  }

  public double getCapacidadVolumen() {
    return capacidadVolumen;
  }

  public double getAltura() {
    return altura;
  }

  public double getCapacidadCarga() {
    return capacidadCarga;
  }
}