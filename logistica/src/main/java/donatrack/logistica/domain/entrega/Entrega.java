package donatrack.logistica.domain.entrega;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Entrega {

  private static long proximoId = 1;

  private final long id;
  private final long donacionId;
  private final long beneficiariaId;
  private final String direccionDestino;
  private final String descripcionDonacion;
  private final double cantidad;
  private final String unidad;

  private Double pesoKg;
  private Double volumenM3;

  private EstadoEntrega estado;
  private RutaReparto ruta;
  private String patenteCamion;
  private LocalDateTime fechaHoraEntrega;
  private List<String> fotos = new ArrayList<>();
  private String motivoNoRecepcion;
  private int intentosDePlanificacion;

  public Entrega(long donacionId,
                 long beneficiariaId,
                 String direccionDestino,
                 String descripcionDonacion,
                 double cantidad,
                 String unidad) {
    if (direccionDestino == null || direccionDestino.isBlank()) {
      throw new IllegalArgumentException("La entrega debe tener una direccion de destino.");
    }
    if (descripcionDonacion == null || descripcionDonacion.isBlank()) {
      throw new IllegalArgumentException("La entrega debe tener una descripcion de la donacion.");
    }
    this.id = proximoId++;
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
      throw new IllegalArgumentException("El peso de la entrega debe ser mayor a cero.");
    }
    if (volumenM3 <= 0) {
      throw new IllegalArgumentException("El volumen de la entrega debe ser mayor a cero.");
    }
    this.pesoKg = pesoKg;
    this.volumenM3 = volumenM3;
  }

  // === Transiciones ===

  public void asignarARuta(RutaReparto ruta) {
    exigirEstado("asignar a una ruta", EstadoEntrega.PENDIENTE);
    if (ruta == null) {
      throw new IllegalArgumentException("Debe indicarse la ruta a la que se asigna la entrega.");
    }
    if (!estaMedida()) {
      throw new IllegalStateException(
          "La entrega " + id + " todavia no tiene medicion registrada."
      );
    }
    this.ruta = ruta;
    this.patenteCamion = ruta.getCamion().getPatente();
    this.estado = EstadoEntrega.PLANIFICADA;
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
      throw new IllegalArgumentException(
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
    throw new IllegalStateException(
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

  public long getId() {
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