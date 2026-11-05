package donatrack.web.vista;

import java.util.List;

public record SeguimientoVista(List<MonitoreoRutaVista> rutas) {

  public static final SeguimientoVista VACIO = new SeguimientoVista(List.of());

  public SeguimientoVista {
    rutas = List.copyOf(rutas);
  }

  public List<MonitoreoRutaVista> getRutas() {
    return rutas;
  }

  public int getTotal() {
    return rutas.size();
  }

  public boolean isVacio() {
    return rutas.isEmpty();
  }

  public MonitoreoRutaVista getPrimera() {
    return rutas.isEmpty() ? null : rutas.get(0);
  }

  public String getPuntosJson() {
    return MapaJson.de(
        rutas.stream()
            .filter(MonitoreoRutaVista::isTieneUbicacion)
            .map(SeguimientoVista::comoPunto)
            .toList()
    );
  }

  private static PuntoDeMapa comoPunto(MonitoreoRutaVista ruta) {
    return new PuntoDeMapa(
        ruta.latitud(),
        ruta.longitud(),
        "Camión " + ruta.patenteCamion(),
        ruta.getEstadoTexto() + " · Avance " + ruta.getAvanceTexto()
            + " · " + ruta.getUltimoReporteTexto()
    );
  }
}
