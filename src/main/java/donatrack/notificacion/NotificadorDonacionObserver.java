package donatrack.notificacion;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;

// Observer concreto — notifica al donante cuando su donacion cambia de estado
public class NotificadorDonacionObserver implements DonacionObserver {

    private final Persona donante;
    private final Notificador servicioNotificaciones;

    public NotificadorDonacionObserver(Persona donante, Notificador servicioNotificaciones) {
        this.donante = donante;
        this.servicioNotificaciones = servicioNotificaciones;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        String mensaje = "Su donacion de [" + donacion.getBienes().stream().map(Bien::getNombre).toList()
                + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo;
        servicioNotificaciones.notificar(donante.getUsuario().getNombre(), mensaje);
    }
}
