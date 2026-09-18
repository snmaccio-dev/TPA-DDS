package donatrack.logistica.controller.dto;

import donatrack.logistica.contrato.Fechas;
import donatrack.logistica.domain.monitoreo.ReporteUbicacion;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.time.LocalDateTime;

public record ReporteUbicacionRequest(
    long rutaId,
    String patenteCamion,
    double latitud,
    double longitud,
    double velocidadKmh,
    String momento
) {

  public ReporteUbicacion aReporte(RutaReparto ruta) {
    LocalDateTime medido = Fechas.aFechaHora(momento);

    return new ReporteUbicacion(
        ruta,
        patenteCamion,
        latitud,
        longitud,
        velocidadKmh,
        medido == null ? LocalDateTime.now() : medido
    );
  }
}
