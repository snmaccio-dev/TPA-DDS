package donatrack.logistica.domain.planificacion;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;

import java.util.List;

public interface GeneradorRutas {

  void solicitarPlanificacion(
      String solicitudId,
      List<Entrega> entregas,
      List<Camion> camiones,
      String urlCallback
  );
}
