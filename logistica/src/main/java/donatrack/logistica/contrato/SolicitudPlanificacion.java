package donatrack.logistica.contrato;

import java.util.List;

public record SolicitudPlanificacion(
    String solicitudId,
    String urlCallback,
    List<EntregaParaPlanificar> entregas,
    List<CamionDisponible> camiones
) {

  public SolicitudPlanificacion {
    if (urlCallback == null || urlCallback.isBlank()) {
      throw new IllegalArgumentException("La solicitud debe indicar la URL de callback.");
    }
    if (entregas == null || entregas.isEmpty()) {
      throw new IllegalArgumentException("La solicitud debe incluir al menos una entrega.");
    }
    if (camiones == null || camiones.isEmpty()) {
      throw new IllegalArgumentException("La solicitud debe incluir al menos un camion.");
    }
    entregas = List.copyOf(entregas);
    camiones = List.copyOf(camiones);
  }
}
