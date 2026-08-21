package donatrack.model.logistica.estado;

import donatrack.model.logistica.Entrega;

import java.util.List;

public class Entregada implements EstadoEntrega {

    @Override
    public void iniciarTraslado(Entrega entrega) {
        throw new IllegalStateException("La entrega ya fue completada.");
    }

    @Override
    public void confirmarRecepcion(Entrega entrega, List<String> fotos) {
        throw new IllegalStateException("La entrega ya fue completada.");
    }

    @Override
    public void marcarNoRecibida(Entrega entrega, String motivo) {
        throw new IllegalStateException("La entrega ya fue completada.");
    }

    @Override
    public void volverAPendiente(Entrega entrega) {
        throw new IllegalStateException("La entrega ya fue completada.");
    }

    @Override
    public String getNombre() {
        return "ENTREGADA";
    }
}
