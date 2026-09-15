package donatrack.logistica.domain.planificacion;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoPlanificacion {

  private final String solicitudId;
  private final List<RutaReparto> rutas;
  private final List<Entrega> entregasNoAsignadas;

  public ResultadoPlanificacion(String solicitudId,
                                List<RutaReparto> rutas,
                                List<Entrega> entregasNoAsignadas) {
    this.solicitudId = solicitudId;
    this.rutas = rutas == null ? new ArrayList<>() : new ArrayList<>(rutas);
    this.entregasNoAsignadas =
        entregasNoAsignadas == null ? new ArrayList<>() : new ArrayList<>(entregasNoAsignadas);
  }

  public String getSolicitudId() {
    return solicitudId;
  }

  public List<RutaReparto> getRutas() {
    return new ArrayList<>(rutas);
  }

  public List<Entrega> getEntregasNoAsignadas() {
    return new ArrayList<>(entregasNoAsignadas);
  }
}
