package donatrack.logistica.controller;

import donatrack.logistica.controller.dto.EntregaDTO;
import donatrack.logistica.controller.dto.IniciarRutaRequest;
import donatrack.logistica.controller.dto.RutaDTO;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.seguridad.ControlDeAcceso;
import donatrack.logistica.seguridad.RolDeAcceso;
import donatrack.logistica.service.GestorChoferes;
import donatrack.logistica.service.GestorEntregas;
import donatrack.logistica.service.GestorRutas;
import donatrack.logistica.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class RutasController {

  private final GestorRutas gestorRutas;
  private final GestorEntregas gestorEntregas;
  private final GestorChoferes gestorChoferes;
  private final ControlDeAcceso control;

  public RutasController(GestorRutas gestorRutas,
                         GestorEntregas gestorEntregas,
                         GestorChoferes gestorChoferes,
                         ControlDeAcceso control) {
    this.gestorRutas = gestorRutas;
    this.gestorEntregas = gestorEntregas;
    this.gestorChoferes = gestorChoferes;
    this.control = control;
  }

  public void registrarRutas(Javalin app) {
    app.get("/rutas", this::todas, RolDeAcceso.ADMINISTRADOR);
    app.get("/rutas/mias", this::mias, RolDeAcceso.CHOFER);
    app.get("/rutas/{id}", this::buscar,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.CHOFER);
    app.get("/rutas/{id}/entregas", this::entregas,
        RolDeAcceso.ADMINISTRADOR, RolDeAcceso.CHOFER);
    app.post("/rutas/{id}/inicio", this::iniciar,
        RolDeAcceso.CHOFER, RolDeAcceso.ADMINISTRADOR);
    app.delete("/rutas/{id}", this::eliminar, RolDeAcceso.ADMINISTRADOR);
  }

  private void todas(Context ctx) {
    ctx.json(RutaDTO.desde(gestorRutas.todas()));
  }

  private void mias(Context ctx) {
    ctx.json(RutaDTO.desde(gestorRutas.paraElChofer(choferDelToken(ctx).getId())));
  }

  // Si quien llama es un chofer, la ruta la inicia el, no quien diga el cuerpo del pedido.
  // Un administrador sigue pudiendo iniciarla en nombre de otro indicando el choferId.
  private Chofer choferDelToken(Context ctx) {
    return control.cuentaDelChofer(ctx)
        .map(gestorChoferes::deLaCuenta)
        .orElseThrow(() ->
            new RecursoInexistenteException("La cuenta no esta vinculada con ningun chofer."));
  }

  private void buscar(Context ctx) {
    ctx.json(RutaDTO.desde(gestorRutas.buscar(idDe(ctx))));
  }

  private void entregas(Context ctx) {
    ctx.json(EntregaDTO.desde(gestorEntregas.deRuta(idDe(ctx))));
  }

  private void iniciar(Context ctx) {
    ctx.json(RutaDTO.desde(gestorEntregas.iniciarRuta(idDe(ctx), choferQueInicia(ctx))));
  }

  private long choferQueInicia(Context ctx) {
    return control.cuentaDelChofer(ctx)
        .map(cuenta -> gestorChoferes.deLaCuenta(cuenta).getId())
        .orElseGet(() -> ctx.bodyAsClass(IniciarRutaRequest.class).choferIdObligatorio());
  }

  private void eliminar(Context ctx) {
    gestorRutas.buscar(idDe(ctx));
    gestorRutas.eliminar(idDe(ctx));
    ctx.status(204);
  }

  private long idDe(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
