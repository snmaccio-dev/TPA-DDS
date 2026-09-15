package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.service.GestorNotificaciones;

// Observer concreto — notifica al donante cuando su donacion cambia de estado
public class NotificadorDonacionObserver implements DonacionObserver {

    private final Donante donante;
    private final GestorNotificaciones gestor;

    public NotificadorDonacionObserver(Donante donante, GestorNotificaciones gestor) {
        this.donante = donante;
        this.gestor = gestor;
    }

    @Override
    public void onCambioEstado(Donacion donacion, String estadoAnterior, String estadoNuevo) {
        String mensaje = "Su donacion de [" + donacion.getSubcategoria().getNombre()
                + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo;
        gestor.notificar(donante.getPersona(), mensaje);
    }
}