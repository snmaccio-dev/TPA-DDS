package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.MonitoreoRutaDTO;
import donatrack.logistica.controller.dto.ReporteUbicacionRequest;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.service.GestorMonitoreo;
import donatrack.logistica.service.GestorRutas;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.List;

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
    RutaReparto ruta = gestorRutas.buscar(request.rutaId());
    gestorMonitoreo.registrarPosicion(request.aReporte(ruta));
    ctx.status(202);
  }

  private void rutasEnCurso(Context ctx) {
    String beneficiaria = ctx.queryParam("beneficiaria");
    String donaciones = ctx.queryParam("donaciones");

    List<RutaReparto> rutas;
    if (beneficiaria != null) {
      rutas = gestorMonitoreo.rutasEnCursoDeBeneficiaria(comoId(beneficiaria, "beneficiaria"));
    } else if (donaciones != null) {
      rutas = gestorMonitoreo.rutasEnCursoDeDonaciones(comoIds(donaciones));
    } else {
      rutas = gestorMonitoreo.rutasEnCurso();
    }

    ctx.json(rutas.stream().map(this::estadoDe).toList());
  }

  private List<Long> comoIds(String valores) {
    return Arrays.stream(valores.split(","))
        .map(String::trim)
        .filter(valor -> !valor.isEmpty())
        .map(valor -> comoId(valor, "donaciones"))
        .toList();
  }

  private long comoId(String valor, String parametro) {
    try {
      return Long.parseLong(valor.trim());
    } catch (NumberFormatException e) {
      throw new DatosInvalidosException(
          "El parametro " + parametro + " debe contener identificadores numericos."
      );
    }
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
