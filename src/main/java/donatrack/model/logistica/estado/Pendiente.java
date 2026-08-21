package donatrack.model.logistica.estado;

import donatrack.model.logistica.Entrega;

import java.util.List;

public class Pendiente implements EstadoEntrega {

    @Override
    public void iniciarTraslado(Entrega entrega) {
        entrega.cambiarEstado(new EnTraslado());
    }

    @Override
    public void confirmarRecepcion(Entrega entrega, List<String> fotos) {
        throw new IllegalStateException("No se puede confirmar recepcion: la entrega aun no fue iniciada.");
    }

    @Override
    public void marcarNoRecibida(Entrega entrega, String motivo) {
        throw new IllegalStateException("No se puede marcar como no recibida: la entrega aun no fue iniciada.");
    }

    @Override
    public void volverAPendiente(Entrega entrega) {
        throw new IllegalStateException("La entrega ya esta pendiente.");
    }

    @Override
    public String getNombre() {
        return "PENDIENTE";
    }
}
