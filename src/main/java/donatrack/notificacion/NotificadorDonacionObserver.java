package donatrack.notificacion;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;
import donatrack.model.donacion.Bien;

// Observer concreto — notifica al donante cuando su donacion cambia de estado
public class NotificadorDonacionObserver implements DonacionObserver {

    private final Persona donante;
    private final Notificador notificador;

    public NotificadorDonacionObserver(Persona donante, Notificador notificador) {
        this.donante = donante;
        this.notificador = notificador;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        String mensaje = "Su donacion de [" + donacion.getBienes().stream().map(Bien::getNombre).toList()
                + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo;
        notificador.notificar(donante.getUsuario().getNombre(), mensaje);
    }
}
