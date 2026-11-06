package donatrack.logistica.seguridad;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.javalin.http.Context;

public interface ControlDeAcceso {

  void exigirAccesoAEntrega(Context ctx, Entrega entrega);

  void exigirAccesoARuta(Context ctx, RutaReparto ruta);

  AlcanceDeMonitoreo alcanceDeMonitoreo(Context ctx, AlcanceDeMonitoreo pedido);
}
