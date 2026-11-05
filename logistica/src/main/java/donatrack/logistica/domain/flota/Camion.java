package donatrack.logistica.domain.flota;

import donatrack.logistica.domain.DatosInvalidosException;

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
    if (patente == null || patente.isBlank()) {
      throw new DatosInvalidosException("El camion debe tener una patente.");
    }

    this.patente = patente;
    actualizarCapacidades(capacidadVolumen, altura, capacidadCarga);
  }

  public void actualizarCapacidades(double capacidadVolumen,
                                    double altura,
                                    double capacidadCarga) {
    validarPositivo(capacidadVolumen, "una capacidad de volumen");
    validarPositivo(altura, "una altura");
    validarPositivo(capacidadCarga, "una capacidad de carga");

    this.capacidadVolumen = capacidadVolumen;
    this.altura = altura;
    this.capacidadCarga = capacidadCarga;
  }

  private static void validarPositivo(double valor, String descripcion) {
    if (valor <= 0) {
      throw new DatosInvalidosException(
          "El camion debe tener " + descripcion + " mayor a cero."
      );
    }
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
