package donatrack.notificacion;

import donatrack.model.donacion.Donacion;

// Observer — contrato para recibir cambios de estado de una Donacion
public interface DonacionObserver {
    void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo);
}
