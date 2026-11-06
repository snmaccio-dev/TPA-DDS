package donatrack.web.vista;

import java.util.List;

public record PlanificacionVista(
    List<RutaVista> rutas,
    List<EntregaVista> pendientes
) {

  public static final PlanificacionVista VACIA = new PlanificacionVista(List.of(), List.of());

  public PlanificacionVista {
    rutas = rutas == null ? List.of() : List.copyOf(rutas);
    pendientes = pendientes == null ? List.of() : List.copyOf(pendientes);
  }

  public List<RutaVista> getRutas() {
    return rutas;
  }

  public List<EntregaVista> getPendientes() {
    return pendientes;
  }

  public boolean isSinRutas() {
    return rutas.isEmpty();
  }

  public boolean isSinPendientes() {
    return pendientes.isEmpty();
  }

  public int getTotalRutas() {
    return rutas.size();
  }

  public int getTotalPendientes() {
    return pendientes.size();
  }

  public long getPlanificadas() {
    return rutas.stream().filter(ruta -> "PLANIFICADA".equals(ruta.estado())).count();
  }

  public long getEnCurso() {
    return rutas.stream().filter(ruta -> "EN_CURSO".equals(ruta.estado())).count();
  }
}
