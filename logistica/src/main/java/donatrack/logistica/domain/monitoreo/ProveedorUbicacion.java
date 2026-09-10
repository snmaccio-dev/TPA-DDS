package donatrack.logistica.domain.monitoreo;

import donatrack.logistica.domain.flota.Camion;

public interface ProveedorUbicacion {
  EstadoRecorrido  obtenerEstado(Camion camion);
}