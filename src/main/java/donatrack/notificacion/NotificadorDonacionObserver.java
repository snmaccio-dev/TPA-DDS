package donatrack.notificacion;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;

// Observer concreto — notifica al donante cuando su donacion cambia de estado
public class NotificadorDonacionObserver implements DonacionObserver {

    private final Persona donante;
    private final ServicioNotificaciones servicioNotificaciones;

    public NotificadorDonacionObserver(Persona donante, ServicioNotificaciones servicioNotificaciones) {
        this.donante = donante;
        this.servicioNotificaciones = servicioNotificaciones;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        String mensaje = "Su donacion de [" + donacion.getSubcategoria().getNombre()
                + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo;
        servicioNotificaciones.notificar(donante, mensaje);
    }
}
