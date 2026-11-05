package donatrack.web.vista;

import java.util.List;

public record FlotaVista(List<CamionVista> camiones) {

  public static final FlotaVista VACIA = new FlotaVista(List.of());

  public FlotaVista {
    camiones = List.copyOf(camiones);
  }

  public List<CamionVista> getCamiones() {
    return camiones;
  }

  public int getTotal() {
    return camiones.size();
  }

  public long getDisponibles() {
    return camiones.stream().filter(CamionVista::isDisponible).count();
  }

  public long getEnRuta() {
    return getTotal() - getDisponibles();
  }

  public String getCargaTotalTexto() {
    return Formato.kilos(
        camiones.stream().mapToDouble(CamionVista::capacidadCarga).sum()
    );
  }

  public boolean isVacia() {
    return camiones.isEmpty();
  }
}
