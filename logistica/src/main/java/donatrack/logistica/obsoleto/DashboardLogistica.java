package donatrack.logistica.obsoleto;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.monitoreo.EstadoRecorrido;

public class DashboardLogistica {

  private ProveedorUbicacion proveedorUbicacion;

  public DashboardLogistica(ProveedorUbicacion proveedorUbicacion) {
    this.proveedorUbicacion = proveedorUbicacion;
  }

  public EstadoRecorrido obtenerEstadoRecorrido(Camion camion) {
    return proveedorUbicacion.obtenerEstado(camion);
  }

  public void cambiarProveedor(ProveedorUbicacion proveedorUbicacion) {
    this.proveedorUbicacion = proveedorUbicacion;
  }
}

