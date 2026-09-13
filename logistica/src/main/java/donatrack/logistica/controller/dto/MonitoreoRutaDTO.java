package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.monitoreo.ReporteUbicacion;
import donatrack.logistica.domain.ruta.RutaReparto;

public record MonitoreoRutaDTO(
    long rutaId,
    String patenteCamion,
    String estadoRuta,
    Double latitud,
    Double longitud,
    Double velocidadKmh,
    double porcentajeAvance,
    String ultimoReporte
) {

  public static MonitoreoRutaDTO desde(RutaReparto ruta, ReporteUbicacion reporte) {
    return new MonitoreoRutaDTO(
        ruta.getId(),
        ruta.getCamion().getPatente(),
        ruta.getEstado().getNombre(),
        reporte.latitud(),
        reporte.longitud(),
        reporte.velocidadKmh(),
        ruta.getPorcentajeAvance(),
        Fechas.aTexto(reporte.momento())
    );
  }

  public static MonitoreoRutaDTO sinReportes(RutaReparto ruta) {
    return new MonitoreoRutaDTO(
        ruta.getId(),
        ruta.getCamion().getPatente(),
        ruta.getEstado().getNombre(),
        null,
        null,
        null,
        ruta.getPorcentajeAvance(),
        null
    );
  }
}
