package donatrack.logistica.repository;

import donatrack.logistica.domain.monitoreo.ReporteUbicacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

public class RepositorioPosiciones implements WithSimplePersistenceUnit {

  private static RepositorioPosiciones instancia;

  private RepositorioPosiciones() {
  }

  public static RepositorioPosiciones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioPosiciones();
    }

    return instancia;
  }

  public void registrar(ReporteUbicacion reporte) {
    persist(reporte);
  }
}
