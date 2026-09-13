package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.MonitoreoRutaDTO;
import donatrack.logistica.controller.dto.ReporteUbicacionRequest;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.service.GestorMonitoreo;
import donatrack.logistica.service.GestorRutas;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class MonitoreoController {

  private final GestorMonitoreo gestorMonitoreo;
  private final GestorRutas gestorRutas;

  public MonitoreoController(GestorMonitoreo gestorMonitoreo, GestorRutas gestorRutas) {
    this.gestorMonitoreo = gestorMonitoreo;
    this.gestorRutas = gestorRutas;
  }

  public void registrarRutas(Javalin app) {
    app.post("/monitoreo/posiciones", this::registrarPosicion);
    app.get("/monitoreo/rutas", this::rutasEnCurso);
    app.get("/monitoreo/rutas/{id}", this::estadoDeRuta);
  }

  private void registrarPosicion(Context ctx) {
    ReporteUbicacionRequest request = ctx.bodyAsClass(ReporteUbicacionRequest.class);
    gestorMonitoreo.registrarPosicion(request.aReporte());
    ctx.status(202);
  }

  private void rutasEnCurso(Context ctx) {
    ctx.json(gestorMonitoreo.rutasEnCurso().stream()
        .map(this::estadoDe)
        .toList());
  }

  private void estadoDeRuta(Context ctx) {
    long rutaId = ctx.pathParamAsClass("id", Long.class).get();
    ctx.json(estadoDe(gestorRutas.buscar(rutaId)));
  }

  private MonitoreoRutaDTO estadoDe(RutaReparto ruta) {
    return gestorMonitoreo.ultimaPosicionDe(ruta.getId())
        .map(reporte -> MonitoreoRutaDTO.desde(ruta, reporte))
        .orElseGet(() -> MonitoreoRutaDTO.sinReportes(ruta));
  }
}
