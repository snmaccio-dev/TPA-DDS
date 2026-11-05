package donatrack.logistica.domain.planificacion;

import donatrack.logistica.domain.ConflictoDeNegocioException;
import donatrack.logistica.domain.DatosInvalidosException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitud_planificacion")
public class SolicitudDePlanificacion {

  @Id
  private String id;

  @Column(name = "fecha_hora_envio", nullable = false)
  private LocalDateTime fechaHoraEnvio;

  @Column(name = "cantidad_de_entregas", nullable = false)
  private int cantidadDeEntregas;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoSolicitud estado;

  @Column(name = "fecha_hora_respuesta")
  private LocalDateTime fechaHoraRespuesta;

  protected SolicitudDePlanificacion() {
  }

  public SolicitudDePlanificacion(int cantidadDeEntregas) {
    if (cantidadDeEntregas <= 0) {
      throw new DatosInvalidosException(
          "La solicitud debe incluir al menos una entrega."
      );
    }

    this.id = UUID.randomUUID().toString();
    this.fechaHoraEnvio = LocalDateTime.now();
    this.cantidadDeEntregas = cantidadDeEntregas;
    this.estado = EstadoSolicitud.ENVIADA;
  }

  public void registrarRespuesta() {
    if (estado == EstadoSolicitud.RESPONDIDA) {
      throw new ConflictoDeNegocioException(
          "La solicitud " + id + " ya fue respondida el "
              + fechaHoraRespuesta + "."
      );
    }

    this.estado = EstadoSolicitud.RESPONDIDA;
    this.fechaHoraRespuesta = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public LocalDateTime getFechaHoraEnvio() {
    return fechaHoraEnvio;
  }

  public int getCantidadDeEntregas() {
    return cantidadDeEntregas;
  }

  public EstadoSolicitud getEstado() {
    return estado;
  }

  public LocalDateTime getFechaHoraRespuesta() {
    return fechaHoraRespuesta;
  }
}
