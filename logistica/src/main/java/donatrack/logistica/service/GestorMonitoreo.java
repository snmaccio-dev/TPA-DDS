package donatrack.logistica.service;

import donatrack.logistica.domain.monitoreo.EstadoRecorrido;
import donatrack.logistica.domain.monitoreo.ReporteUbicacion;
import donatrack.logistica.domain.ruta.EstadoRuta;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioPosiciones;
import donatrack.logistica.repository.RepositorioRutas;

import java.util.List;
import java.util.Optional;

public class GestorMonitoreo {

  private final RepositorioPosiciones repositorioPosiciones =
      RepositorioPosiciones.getInstance();
  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();

  public void registrarPosicion(ReporteUbicacion reporte) {
    RutaReparto ruta = buscarRuta(reporte.rutaId());

    if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
      throw new IllegalStateException(
          "La ruta " + ruta.getId() + " no esta en curso: no admite reportes de ubicacion."
      );
    }
    if (!ruta.getCamion().getPatente().equals(reporte.patenteCamion())) {
      throw new IllegalArgumentException(
          "La patente reportada no coincide con el camion asignado a la ruta " + ruta.getId() + "."
      );
    }

    repositorioPosiciones.registrar(reporte);
  }

  public EstadoRecorrido estadoDe(long rutaId) {
    RutaReparto ruta = buscarRuta(rutaId);

    ReporteUbicacion ultimo = repositorioPosiciones.ultimaDe(rutaId)
        .orElseThrow(() ->
            new IllegalStateException(
                "Todavia no se recibieron posiciones para la ruta " + rutaId + "."
            ));

    return new EstadoRecorrido(
        ultimo.posicion(),
        ultimo.velocidadKmh(),
        ruta.getPorcentajeAvance()
    );
  }

  public Optional<ReporteUbicacion> ultimaPosicionDe(long rutaId) {
    return repositorioPosiciones.ultimaDe(rutaId);
  }

  public List<RutaReparto> rutasEnCurso() {
    return repositorioRutas.todas().stream()
        .filter(ruta -> ruta.getEstado() == EstadoRuta.EN_CURSO)
        .toList();
  }

  private RutaReparto buscarRuta(long rutaId) {
    return repositorioRutas.buscarPorId(rutaId)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe una ruta con ID " + rutaId
            ));
  }
}
