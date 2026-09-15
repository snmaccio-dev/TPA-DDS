package donatrack.logistica.infrastructure.planificacion;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.planificacion.GeneradorRutas;

import java.util.List;
import java.util.UUID;

public class GeneradorRutasSimulado implements GeneradorRutas {

  @Override
  public String solicitarPlanificacion(
      List<Entrega> entregas,
      List<Camion> camiones,
      String urlCallback) {

    String solicitudId = UUID.randomUUID().toString();

    System.out.println("[PLANIFICADOR SIMULADO] solicitud " + solicitudId
        + " | entregas: " + entregas.stream().map(Entrega::getId).toList()
        + " | camiones: " + camiones.stream().map(Camion::getPatente).toList()
        + " | callback: " + urlCallback);

    return solicitudId;
  }
}
