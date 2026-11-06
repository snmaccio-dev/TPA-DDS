package donatrack.logistica.seguridad;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.javalin.http.Context;

import java.util.Optional;

public interface ControlDeAcceso {

  // Solo devuelve la cuenta cuando quien llama es un chofer: es el unico caso en que
  // Logistica puede deducir sobre quien actuar sin que el cliente se lo diga.
  Optional<String> cuentaDelChofer(Context ctx);

  void exigirAccesoAEntrega(Context ctx, Entrega entrega);

  void exigirAccesoARuta(Context ctx, RutaReparto ruta);

  AlcanceDeMonitoreo alcanceDeMonitoreo(Context ctx, AlcanceDeMonitoreo pedido);
}
