package donatrack.logistica.seguridad;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.javalin.http.Context;

public class ControlDeAccesoAbierto implements ControlDeAcceso {

  @Override
  public void exigirAccesoAEntrega(Context ctx, Entrega entrega) {
  }

  @Override
  public void exigirAccesoARuta(Context ctx, RutaReparto ruta) {
  }

  @Override
  public AlcanceDeMonitoreo alcanceDeMonitoreo(Context ctx, AlcanceDeMonitoreo pedido) {
    return pedido;
  }
}
