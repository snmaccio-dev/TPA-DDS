package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;
import donatrack.model.logistica.estado.EstadoEntrega;
import donatrack.model.logistica.estado.Pendiente;
import donatrack.model.persona.Beneficiaria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Entrega {

  private static long proximoId = 1;

  private final long id;
  private final Donacion donacion;
  private final Beneficiaria destinatario;
  private Camion camion;
  private EstadoEntrega estado;
  private LocalDate fechaEntrega;
  private List<String> fotos = new ArrayList<>();
  private String motivoNoRecibida;

  public Entrega(Donacion donacion,
                 Beneficiaria destinatario,
                 Camion camion) {
    if (donacion == null) {
      throw new IllegalArgumentException("La entrega debe referenciar a una donacion.");
    }
    if (destinatario == null) {
      throw new IllegalArgumentException("La entrega debe tener una beneficiaria destinataria.");
    }
    if (camion == null) {
      throw new IllegalArgumentException("La entrega debe tener un camion asignado.");
    }
    this.id = proximoId++;
    this.donacion = donacion;
    this.destinatario = destinatario;
    this.camion = camion;
    this.estado = new Pendiente();
  }

  // === Delegación al State ===

  public void iniciarTraslado() {
    estado.iniciarTraslado(this);
  }

  public void confirmarRecepcion(List<String> fotos) {
    estado.confirmarRecepcion(this, fotos);
    donacion.marcarEntregada();
  }

  public void marcarNoRecibida(String motivo) {
    estado.marcarNoRecibida(this, motivo);
  }

  public void volverAPendiente() {
    estado.volverAPendiente(this);
  }

  // === Hooks para los estados (visibilidad de paquete) ===

  public void cambiarEstado(EstadoEntrega nuevoEstado) {
    this.estado = nuevoEstado;
  }

  public void registrarRecepcion(List<String> fotos) {
    this.fechaEntrega = LocalDate.now();
    this.fotos = new ArrayList<>(fotos);
    destinatario.registrarDonacionRecibida(donacion);
  }

  public void registrarMotivoNoRecibida(String motivo) {
    this.motivoNoRecibida = motivo;
  }

  public void limpiarMotivoNoRecibida() {
    this.motivoNoRecibida = null;
  }

  // === Getters ===

  public long getId() {
    return id;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public Beneficiaria getDestinatario() {
    return destinatario;
  }

  public Camion getCamion() {
    return camion;
  }

  public EstadoEntrega getEstado() {
    return estado;
  }

  public LocalDate getFechaEntrega() {
    return fechaEntrega;
  }

  public List<String> getFotos() {
    return fotos;
  }

  public String getMotivoNoRecibida() {
    return motivoNoRecibida;
  }
}
