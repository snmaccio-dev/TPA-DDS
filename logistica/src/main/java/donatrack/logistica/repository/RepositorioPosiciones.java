package donatrack.logistica.repository;

import donatrack.logistica.domain.monitoreo.ReporteUbicacion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioPosiciones {

  private static RepositorioPosiciones instancia;

  private final Map<Long, ReporteUbicacion> ultimaPorRuta = new HashMap<>();

  private RepositorioPosiciones() {
  }

  public static RepositorioPosiciones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioPosiciones();
    }

    return instancia;
  }

  public void registrar(ReporteUbicacion reporte) {
    ultimaPorRuta.merge(
        reporte.rutaId(),
        reporte,
        (anterior, nuevo) -> nuevo.momento().isBefore(anterior.momento()) ? anterior : nuevo
    );
  }

  public Optional<ReporteUbicacion> ultimaDe(long rutaId) {
    return Optional.ofNullable(ultimaPorRuta.get(rutaId));
  }

  public void olvidar(long rutaId) {
    ultimaPorRuta.remove(rutaId);
  }
}
