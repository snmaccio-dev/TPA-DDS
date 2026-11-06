package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.MonitoreoRutaDTO;
import donatrack.logistica.controller.dto.ReporteUbicacionRequest;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.seguridad.AlcanceDeMonitoreo;
import donatrack.logistica.seguridad.ControlDeAcceso;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.service.GestorMonitoreo;
import donatrack.logistica.service.GestorRutas;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.List;

public class MonitoreoController {

  private final GestorMonitoreo gestorMonitoreo;
  private final GestorRutas gestorRutas;
  private final ControlDeAcceso control;

  public MonitoreoController(GestorMonitoreo gestorMonitoreo,
                             GestorRutas gestorRutas,
                             ControlDeAcceso control) {
    this.gestorMonitoreo = gestorMonitoreo;
    this.gestorRutas = gestorRutas;
    this.control = control;
  }

  public void registrarRutas(Javalin app) {
    app.post("/monitoreo/posiciones", this::registrarPosicion,
        RolDeAcceso.CHOFER, RolDeAcceso.SERVICIO_LOGISTICA);
    app.get("/monitoreo/rutas", this::rutasEnCurso,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA);
    app.get("/monitoreo/rutas/{id}", this::estadoDeRuta,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.CHOFER,
        RolDeAcceso.DONANTE, RolDeAcceso.BENEFICIARIA);
  }

  private void registrarPosicion(Context ctx) {
    ReporteUbicacionRequest request = ctx.bodyAsClass(ReporteUbicacionRequest.class);
    RutaReparto ruta = gestorRutas.buscar(request.rutaId());
    gestorMonitoreo.registrarPosicion(request.aReporte(ruta));
    ctx.status(202);
  }

  private void rutasEnCurso(Context ctx) {
    AlcanceDeMonitoreo alcance = control.alcanceDeMonitoreo(ctx, alcancePedido(ctx));

    List<RutaReparto> rutas;
    if (alcance.esDeBeneficiaria()) {
      rutas = gestorMonitoreo.rutasEnCursoDeBeneficiaria(alcance.beneficiariaId());
    } else if (alcance.esDeDonante()) {
      rutas = gestorMonitoreo.rutasEnCursoDeDonante(alcance.donanteId());
    } else if (alcance.esDeDonaciones()) {
      rutas = gestorMonitoreo.rutasEnCursoDeDonaciones(alcance.donacionIds());
    } else {
      rutas = gestorMonitoreo.rutasEnCurso();
    }

    ctx.json(rutas.stream().map(this::estadoDe).toList());
  }

  private AlcanceDeMonitoreo alcancePedido(Context ctx) {
    Long beneficiaria = comoIdOpcional(ctx.queryParam("beneficiaria"), "beneficiaria");
    Long donante = comoIdOpcional(ctx.queryParam("donante"), "donante");

    if (beneficiaria != null) {
      return AlcanceDeMonitoreo.deBeneficiaria(beneficiaria);
    }
    if (donante != null) {
      return AlcanceDeMonitoreo.deDonante(donante);
    }

    List<Long> donaciones = comoIds(ctx.queryParam("donaciones"));

    return donaciones.isEmpty()
        ? AlcanceDeMonitoreo.todas()
        : AlcanceDeMonitoreo.deDonaciones(donaciones);
  }

  private Long comoIdOpcional(String valor, String parametro) {
    return valor == null || valor.isBlank() ? null : comoId(valor, parametro);
  }

  private List<Long> comoIds(String valores) {
    if (valores == null || valores.isBlank()) {
      return List.of();
    }

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
    RutaReparto ruta = gestorRutas.buscar(ctx.pathParamAsClass("id", Long.class).get());
    control.exigirAccesoARuta(ctx, ruta);

    ctx.json(estadoDe(ruta));
  }

  private MonitoreoRutaDTO estadoDe(RutaReparto ruta) {
    return gestorMonitoreo.ultimaPosicionDe(ruta.getId())
        .map(reporte -> MonitoreoRutaDTO.desde(ruta, reporte))
        .orElseGet(() -> MonitoreoRutaDTO.sinReportes(ruta));
  }
}
