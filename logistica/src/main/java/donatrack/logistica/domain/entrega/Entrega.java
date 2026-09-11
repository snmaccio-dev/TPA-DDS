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
  private final double pesoKg;
  private final double volumenM3;

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
                 double pesoKg,
                 double volumenM3) {
    if (direccionDestino == null || direccionDestino.isBlank()) {
      throw new IllegalArgumentException("La entrega debe tener una direccion de destino.");
    }
    if (descripcionDonacion == null || descripcionDonacion.isBlank()) {
      throw new IllegalArgumentException("La entrega debe tener una descripcion de la donacion.");
    }
    if (pesoKg < 0 || volumenM3 < 0) {
      throw new IllegalArgumentException("El peso y el volumen de la entrega no pueden ser negativos.");
    }
    this.id = proximoId++;
    this.donacionId = donacionId;
    this.beneficiariaId = beneficiariaId;
    this.direccionDestino = direccionDestino;
    this.descripcionDonacion = descripcionDonacion;
    this.pesoKg = pesoKg;
    this.volumenM3 = volumenM3;
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public static Entrega desde(DonacionParaPlanificar donacion) {
    return new Entrega(
        donacion.donacionId(),
        donacion.beneficiariaId(),
        donacion.direccionDestino(),
        donacion.descripcion(),
        donacion.pesoKg(),
        donacion.volumenM3()
    );
  }

  // === Transiciones ===

  public void asignarARuta(RutaReparto ruta) {
    exigirEstado("asignar a una ruta", EstadoEntrega.PENDIENTE);
    if (ruta == null) {
      throw new IllegalArgumentException("Debe indicarse la ruta a la que se asigna la entrega.");
    }
    this.ruta = ruta;
    this.patenteCamion = ruta.getCamion().getPatente();
  }

  public void marcarEnTraslado() {
    exigirEstado("marcar en traslado", EstadoEntrega.PENDIENTE);
    if (ruta == null) {
      throw new IllegalStateException(
          "La entrega " + id + " todavia no fue incluida en una ruta planificada."
      );
    }
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
    return estado == EstadoEntrega.PENDIENTE && ruta == null;
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

  public double getPesoKg() {
    return pesoKg;
  }

  public double getVolumenM3() {
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
