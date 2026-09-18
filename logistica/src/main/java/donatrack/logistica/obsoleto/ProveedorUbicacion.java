package donatrack.logistica.obsoleto;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.monitoreo.EstadoRecorrido;

public interface ProveedorUbicacion {
  EstadoRecorrido  obtenerEstado(Camion camion);
}
