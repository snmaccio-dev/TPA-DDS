package donatrack.logistica.domain.entrega;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.TransicionNoPermitidaException;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "entrega")
public class Entrega {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "donacion_id", nullable = false)
  private long donacionId;

  @Column(name = "beneficiaria_id", nullable = false)
  private long beneficiariaId;

  @Column(name = "direccion_destino", nullable = false)
  private String direccionDestino;

  @Column(name = "descripcion_donacion", nullable = false)
  private String descripcionDonacion;

  @Column(nullable = false)
  private double cantidad;

  private String unidad;

  @Column(name = "peso_kg")
  private Double pesoKg;

  @Column(name = "volumen_m3")
  private Double volumenM3;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoEntrega estado;

  @ManyToOne
  @JoinColumn(name = "ruta_id")
  private RutaReparto ruta;

  @Column(name = "orden_destino")
  private Integer ordenDestino;

  @Column(name = "patente_camion")
  private String patenteCamion;

  @Column(name = "fecha_hora_entrega")
  private LocalDateTime fechaHoraEntrega;

  @ElementCollection
  @CollectionTable(name = "entrega_foto", joinColumns = @JoinColumn(name = "entrega_id"))
  @Column(name = "url")
  private List<String> fotos = new ArrayList<>();

  @Column(name = "motivo_no_recepcion")
  private String motivoNoRecepcion;

  @Column(name = "intentos_de_planificacion", nullable = false)
  private int intentosDePlanificacion;

  protected Entrega() {
  }

  public Entrega(long donacionId,
                 long beneficiariaId,
                 String direccionDestino,
                 String descripcionDonacion,
                 double cantidad,
                 String unidad) {
    if (direccionDestino == null || direccionDestino.isBlank()) {
      throw new DatosInvalidosException("La entrega debe tener una direccion de destino.");
    }
    if (descripcionDonacion == null || descripcionDonacion.isBlank()) {
      throw new DatosInvalidosException("La entrega debe tener una descripcion de la donacion.");
    }
    this.donacionId = donacionId;
    this.beneficiariaId = beneficiariaId;
    this.direccionDestino = direccionDestino;
    this.descripcionDonacion = descripcionDonacion;
    this.cantidad = cantidad;
    this.unidad = unidad;
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public static Entrega desde(DonacionParaPlanificar donacion) {
    return new Entrega(
        donacion.donacionId(),
        donacion.beneficiariaId(),
        donacion.direccionDestino(),
        donacion.descripcion(),
        donacion.cantidad(),
        donacion.unidad()
    );
  }

  // === Medicion ===

  public boolean estaMedida() {
    return pesoKg != null && volumenM3 != null;
  }

  public void registrarMedicion(double pesoKg, double volumenM3) {
    exigirEstado("registrar la medicion", EstadoEntrega.PENDIENTE);
    if (pesoKg <= 0) {
      throw new DatosInvalidosException("El peso de la entrega debe ser mayor a cero.");
    }
    if (volumenM3 <= 0) {
      throw new DatosInvalidosException("El volumen de la entrega debe ser mayor a cero.");
    }
    this.pesoKg = pesoKg;
    this.volumenM3 = volumenM3;
  }

  // === Transiciones ===

  public void asignarARuta(RutaReparto ruta) {
    exigirEstado("asignar a una ruta", EstadoEntrega.PENDIENTE);
    Objects.requireNonNull(ruta, "Debe indicarse la ruta a la que se asigna la entrega.");
    if (!estaMedida()) {
      throw new TransicionNoPermitidaException(
          "La entrega " + id + " todavia no tiene medicion registrada."
      );
    }
    this.ruta = ruta;
    this.patenteCamion = ruta.getCamion().getPatente();
    this.estado = EstadoEntrega.PLANIFICADA;
  }

  public void asignarADestino(int orden) {
    if (orden < 0) {
      throw new DatosInvalidosException("El orden del destino no puede ser negativo.");
    }
    this.ordenDestino = orden;
  }

  public void desasignarDeRuta() {
    exigirEstado("desasignar de la ruta", EstadoEntrega.PLANIFICADA);
    this.ruta = null;
    this.ordenDestino = null;
    this.patenteCamion = null;
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public void marcarEnTraslado() {
    exigirEstado("marcar en traslado", EstadoEntrega.PLANIFICADA);
    this.estado = EstadoEntrega.EN_TRASLADO;
  }

  public void confirmarRecepcion(List<String> fotos) {
    exigirEstado("confirmar la recepcion", EstadoEntrega.EN_TRASLADO);
    this.fechaHoraEntrega = LocalDateTime.now();
    this.fotos = fotos == null ? new ArrayList<>() : new ArrayList<>(fotos);
    this.estado = EstadoEntrega.ENTREGADA;
  }

  public void marcarNoRecibida(String motivo) {
    exigirEstado("marcar como no recibida", EstadoEntrega.EN_TRASLADO);
    if (motivo == null || motivo.isBlank()) {
      throw new DatosInvalidosException(
          "Debe registrarse el motivo por el que la entrega no fue recibida."
      );
    }
    this.motivoNoRecepcion = motivo;
    this.fechaHoraEntrega = LocalDateTime.now();
    this.estado = EstadoEntrega.NO_RECIBIDA;
  }

  public void retornarADeposito() {
    exigirEstado("retornar al deposito", EstadoEntrega.NO_RECIBIDA);
    this.ruta = null;
    this.ordenDestino = null;
    this.patenteCamion = null;
    this.fechaHoraEntrega = null;
    this.motivoNoRecepcion = null;
    this.fotos = new ArrayList<>();
    this.estado = EstadoEntrega.PENDIENTE;
  }

  private void exigirEstado(String accion, EstadoEntrega... permitidos) {
    for (EstadoEntrega permitido : permitidos) {
      if (estado == permitido) {
        return;
      }
    }
    throw new TransicionNoPermitidaException(
        "No se puede " + accion + " desde el estado " + estado.getNombre() + "."
    );
  }

  // === Planificacion ===

  public boolean esperaPlanificacion() {
    return estado == EstadoEntrega.PENDIENTE && estaMedida();
  }

  public void registrarIntentoDePlanificacion() {
    intentosDePlanificacion++;
  }

  public int getIntentosDePlanificacion() {
    return intentosDePlanificacion;
  }

  // === Getters ===

  public Long getId() {
    return id;
  }

  public long getDonacionId() {
    return donacionId;
  }

  public long getBeneficiariaId() {
    return beneficiariaId;
  }

  public String getDireccionDestino() {
    return direccionDestino;
  }

  public String getDescripcionDonacion() {
    return descripcionDonacion;
  }

  public double getCantidad() {
    return cantidad;
  }

  public String getUnidad() {
    return unidad;
  }

  public Double getPesoKg() {
    return pesoKg;
  }

  public Double getVolumenM3() {
    return volumenM3;
  }

  public EstadoEntrega getEstado() {
    return estado;
  }

  public Integer getOrdenDestino() {
    return ordenDestino;
  }

  public RutaReparto getRuta() {
    return ruta;
  }

  public String getPatenteCamion() {
    return patenteCamion;
  }

  public LocalDateTime getFechaHoraEntrega() {
    return fechaHoraEntrega;
  }

  public List<String> getFotos() {
    return new ArrayList<>(fotos);
  }

  public String getMotivoNoRecepcion() {
    return motivoNoRecepcion;
  }

  @Override
  public String toString() {
    return "Entrega[id=" + id
        + ", donacionId=" + donacionId
        + ", estado=" + estado.getNombre()
        + ", destino=" + direccionDestino + "]";
  }
}