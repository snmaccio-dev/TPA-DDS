package donatrack.logistica.domain.monitoreo;

import donatrack.logistica.domain.ruta.RutaReparto;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "reporte_ubicacion")
public class ReporteUbicacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "ruta_id", nullable = false)
  private RutaReparto ruta;

  @Column(name = "patente_camion", nullable = false)
  private String patenteCamion;

  @Column(nullable = false)
  private double latitud;

  @Column(nullable = false)
  private double longitud;

  @Column(name = "velocidad_kmh", nullable = false)
  private double velocidadKmh;

  @Column(nullable = false)
  private LocalDateTime momento;

  protected ReporteUbicacion() {
  }

  public ReporteUbicacion(RutaReparto ruta,
                          String patenteCamion,
                          double latitud,
                          double longitud,
                          double velocidadKmh,
                          LocalDateTime momento) {
    if (ruta == null) {
      throw new IllegalArgumentException("El reporte debe indicar la ruta a la que corresponde.");
    }
    if (patenteCamion == null || patenteCamion.isBlank()) {
      throw new IllegalArgumentException("El reporte debe indicar la patente del camion.");
    }
    if (latitud < -90 || latitud > 90) {
      throw new IllegalArgumentException("La latitud reportada esta fuera de rango.");
    }
    if (longitud < -180 || longitud > 180) {
      throw new IllegalArgumentException("La longitud reportada esta fuera de rango.");
    }
    if (velocidadKmh < 0) {
      throw new IllegalArgumentException("La velocidad reportada no puede ser negativa.");
    }
    if (momento == null) {
      throw new IllegalArgumentException("El reporte debe indicar el momento de la medicion.");
    }
    this.ruta = ruta;
    this.patenteCamion = patenteCamion;
    this.latitud = latitud;
    this.longitud = longitud;
    this.velocidadKmh = velocidadKmh;
    this.momento = momento;
  }

  public Posicion getPosicion() {
    return new Posicion(latitud, longitud);
  }

  public Long getId() {
    return id;
  }

  public RutaReparto getRuta() {
    return ruta;
  }

  public String getPatenteCamion() {
    return patenteCamion;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }

  public double getVelocidadKmh() {
    return velocidadKmh;
  }

  public LocalDateTime getMomento() {
    return momento;
  }
}
