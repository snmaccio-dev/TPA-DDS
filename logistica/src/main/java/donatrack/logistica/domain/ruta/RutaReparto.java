package donatrack.logistica.domain.ruta;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.entrega.EstadoEntrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.monitoreo.ReporteUbicacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "ruta_reparto")
public class RutaReparto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "camion_id", nullable = false)
  private Camion camion;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ruta_id", nullable = false)
  @OrderBy("orden")
  private List<DestinoEntrega> destinos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoRuta estado;

  @ManyToOne
  @JoinColumn(name = "chofer_id")
  private Chofer chofer;

  @Column(name = "fecha_hora_inicio")
  private LocalDateTime fechaHoraInicio;

  @OneToOne
  @JoinColumn(name = "ultimo_reporte_id")
  private ReporteUbicacion ultimoReporte;

  protected RutaReparto() {
  }

  public RutaReparto(Camion camion, List<DestinoEntrega> destinos) {
    if (camion == null) {
      throw new IllegalArgumentException("La ruta debe tener un camion asignado.");
    }
    if (destinos == null || destinos.isEmpty()) {
      throw new IllegalArgumentException("La ruta debe tener al menos un destino.");
    }
    this.camion = camion;
    this.destinos = new ArrayList<>(destinos);
    this.estado = EstadoRuta.PLANIFICADA;
    getEntregas().forEach(entrega -> entrega.asignarARuta(this));
  }

  // === Recorrido ===

  public List<Long> iniciarRuta(Chofer chofer) {
    if (estado != EstadoRuta.PLANIFICADA) {
      throw new IllegalStateException(
          "La ruta " + id + " ya fue iniciada."
      );
    }
    if (chofer == null) {
      throw new IllegalArgumentException("Debe indicarse el chofer que inicia la ruta.");
    }
    this.chofer = chofer;
    this.fechaHoraInicio = LocalDateTime.now();
    this.estado = EstadoRuta.EN_CURSO;

    List<Entrega> entregas = getEntregas();
    entregas.forEach(Entrega::marcarEnTraslado);

    return entregas.stream()
        .map(Entrega::getDonacionId)
        .toList();
  }

  public void finalizarSiNoQuedanEntregasEnTraslado() {
    boolean quedanEnTraslado = getEntregas().stream()
        .anyMatch(entrega -> entrega.getEstado() == EstadoEntrega.EN_TRASLADO);

    if (estado == EstadoRuta.EN_CURSO && !quedanEnTraslado) {
      this.estado = EstadoRuta.FINALIZADA;
    }
  }

  public void liberarEntregas() {
    getEntregas().forEach(Entrega::desasignarDeRuta);
  }

  public void registrarUbicacion(ReporteUbicacion reporte) {
    if (reporte == null) {
      throw new IllegalArgumentException("Debe indicarse el reporte de ubicacion.");
    }
    if (ultimoReporte == null || !reporte.getMomento().isBefore(ultimoReporte.getMomento())) {
      this.ultimoReporte = reporte;
    }
  }

  public double getPorcentajeAvance() {
    List<Entrega> entregas = getEntregas();
    if (entregas.isEmpty()) {
      return 0;
    }
    long resueltas = entregas.stream()
        .filter(entrega -> entrega.getEstado() == EstadoEntrega.ENTREGADA
            || entrega.getEstado() == EstadoEntrega.NO_RECIBIDA)
        .count();

    return (resueltas * 100.0) / entregas.size();
  }

  // === Getters ===

  public Long getId() {
    return id;
  }

  public Camion getCamion() {
    return camion;
  }

  public Chofer getChofer() {
    return chofer;
  }

  public EstadoRuta getEstado() {
    return estado;
  }

  public LocalDateTime getFechaHoraInicio() {
    return fechaHoraInicio;
  }

  public ReporteUbicacion getUltimoReporte() {
    return ultimoReporte;
  }

  public List<DestinoEntrega> getDestinos() {
    return new ArrayList<>(destinos);
  }

  public List<Entrega> getEntregas() {
    return destinos.stream()
        .flatMap(destino -> destino.getEntregas().stream())
        .toList();
  }
}
