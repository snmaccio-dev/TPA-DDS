package donatrack.logistica.infrastructure.planificacion;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.planificacion.GeneradorRutas;

import java.util.List;

public class GeneradorRutasSimulado implements GeneradorRutas {

  @Override
  public void solicitarPlanificacion(
      String solicitudId,
      List<Entrega> entregas,
      List<Camion> camiones,
      String urlCallback) {

    System.out.println("[PLANIFICADOR SIMULADO] solicitud " + solicitudId
        + " | entregas: " + entregas.stream().map(Entrega::getId).toList()
        + " | camiones: " + camiones.stream().map(Camion::getPatente).toList()
        + " | callback: " + urlCallback);
  }
}
