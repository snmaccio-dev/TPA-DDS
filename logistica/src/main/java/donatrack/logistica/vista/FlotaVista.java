package donatrack.logistica.vista;

import java.util.List;

public record FlotaVista(List<CamionVista> camiones) {

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
    return CamionVista.kilos(
        camiones.stream().mapToDouble(CamionVista::capacidadCarga).sum()
    );
  }

  public boolean isVacia() {
    return camiones.isEmpty();
  }
}
