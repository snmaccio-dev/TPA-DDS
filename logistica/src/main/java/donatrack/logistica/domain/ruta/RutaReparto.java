package donatrack.logistica.domain.ruta;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.entrega.EstadoEntrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RutaReparto {

  private static long proximoId = 1;

  private final long id;
  private final Camion camion;
  private final List<DestinoEntrega> destinos;

  private EstadoRuta estado;
  private Chofer chofer;
  private LocalDateTime fechaHoraInicio;

  public RutaReparto(Camion camion, List<DestinoEntrega> destinos) {
    if (camion == null) {
      throw new IllegalArgumentException("La ruta debe tener un camion asignado.");
    }
    if (destinos == null || destinos.isEmpty()) {
      throw new IllegalArgumentException("La ruta debe tener al menos un destino.");
    }
    this.id = proximoId++;
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

  public long getId() {
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

  public List<DestinoEntrega> getDestinos() {
    return new ArrayList<>(destinos);
  }

  public List<Entrega> getEntregas() {
    return destinos.stream()
        .flatMap(destino -> destino.getEntregas().stream())
        .toList();
  }
}
