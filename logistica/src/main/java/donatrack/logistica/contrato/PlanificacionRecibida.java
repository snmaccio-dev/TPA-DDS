package donatrack.logistica.contrato;

import java.util.List;

public record PlanificacionRecibida(
    String solicitudId,
    List<RutaPlanificada> rutas,
    List<Long> entregaIdsNoAsignadas
) {

  public PlanificacionRecibida {
    rutas = rutas == null ? List.of() : List.copyOf(rutas);
    entregaIdsNoAsignadas =
        entregaIdsNoAsignadas == null ? List.of() : List.copyOf(entregaIdsNoAsignadas);
  }
}
