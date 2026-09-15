package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;

// Observer — contrato para recibir cambios de estado de una Donacion
public interface DonacionObserver {
    void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo);
}
