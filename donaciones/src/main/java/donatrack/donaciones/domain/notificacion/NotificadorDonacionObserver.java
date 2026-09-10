package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Donante;

// Observer concreto — notifica al donante cuando su donacion cambia de estado
public class NotificadorDonacionObserver implements DonacionObserver {

    private final Donante donante;
    private final Notificador notificador;

    public NotificadorDonacionObserver(Donante donante, Notificador notificador) {
        this.donante = donante;
        this.notificador = notificador;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        String mensaje = "Su donacion de [" + donacion.getSubcategoria().getNombre()
                + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo;
        notificador.notificar(donante.getPersona().getUsuario().getNombre(), mensaje);
    }
}
