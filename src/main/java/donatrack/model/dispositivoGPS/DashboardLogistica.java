package donatrack.model.dispositivoGPS;

import donatrack.model.logistica.Camion;

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

