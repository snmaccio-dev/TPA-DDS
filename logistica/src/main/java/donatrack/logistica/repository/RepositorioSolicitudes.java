package donatrack.logistica.repository;

import donatrack.logistica.domain.planificacion.SolicitudDePlanificacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.Optional;

public class RepositorioSolicitudes implements WithSimplePersistenceUnit {

  private static RepositorioSolicitudes instancia;

  private RepositorioSolicitudes() {
  }

  public static RepositorioSolicitudes getInstance() {
    if (instancia == null) {
      instancia = new RepositorioSolicitudes();
    }

    return instancia;
  }

  public void guardar(SolicitudDePlanificacion solicitud) {
    persist(solicitud);
  }

  public Optional<SolicitudDePlanificacion> buscarPorId(String id) {
    return id == null
        ? Optional.empty()
        : Optional.ofNullable(find(SolicitudDePlanificacion.class, id));
  }
}
