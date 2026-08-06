package donatrack.model.dispositivoGPS;

import donatrack.model.logistica.Camion;

public interface ProveedorUbicacion {
  EstadoRecorrido  obtenerEstado(Camion camion);
}