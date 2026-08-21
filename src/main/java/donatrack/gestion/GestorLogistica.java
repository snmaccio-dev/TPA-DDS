package donatrack.gestion;

import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.estado.AsignacionRealizada;
import donatrack.model.logistica.Camion;
import donatrack.model.logistica.GeneradorRutas;
import donatrack.model.logistica.RutaReparto;
import donatrack.model.planificacion.ResultadoPlanificacion;
import donatrack.repositorio.RepositorioRutas;

import java.util.List;
import java.util.stream.IntStream;

public class GestorLogistica {

  private final GeneradorRutas generadorRutas;
  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();

  public GestorLogistica(GeneradorRutas generadorRutas) {
    this.generadorRutas = generadorRutas;
  }

  public void planificarRutas(
      List<Donacion> donaciones,
      List<Camion> camiones) {

    List<Donacion> asignadas = donaciones.stream()
        .filter(d ->
            d.getEstado() instanceof AsignacionRealizada)
        .toList();

    List<List<Donacion>> lotes =
        dividirEnLotes(asignadas);

    lotes.forEach(lote ->
        generadorRutas.generar(lote, camiones)
    );
  }

  public void procesarResultadoPlanificacion(
      List<RutaReparto> rutas,
      List<Donacion> noAsignadas) {

    repositorioRutas.guardar(rutas);

    rutas.forEach(this::asignarCamionYPlanificar);

    // Las no asignadas deberán volver a ser planificadas.
    if (!noAsignadas.isEmpty()) {
      // Replanificación pendiente
    }
  }

  private List<List<Donacion>> dividirEnLotes(
      List<Donacion> donaciones) {

    int cantidadLotes = (donaciones.size() + 99) / 100;

    return IntStream.range(0, cantidadLotes)
        .mapToObj(i ->
            donaciones.subList(
                i * 100,
                Math.min((i + 1) * 100, donaciones.size())
            )
        )
        .toList();
  }

  public void procesarResultadoPlanificacion(
      ResultadoPlanificacion resultado,
      List<Camion> camiones) {

    repositorioRutas.guardar(resultado.getRutas());

    resultado.getRutas().forEach(this::asignarCamionYPlanificar);

    List<Donacion> noAsignadas =
        resultado.getDonacionesNoAsignadas();

    if (!noAsignadas.isEmpty()) {
      planificarRutas(noAsignadas, camiones);
    }
  }

  private void asignarCamionYPlanificar(RutaReparto ruta) {
    ruta.getDestinos().forEach(destino ->
        destino.getDonaciones().forEach(donacion -> {
          donacion.asignarCamion(ruta.getCamion());
          donacion.marcarListaParaEntregar();
        })
    );
  }
}
