package donatrack.model.logistica.estado;

import donatrack.model.logistica.Entrega;

import java.util.List;

public class NoRecibida implements EstadoEntrega {

    @Override
    public void iniciarTraslado(Entrega entrega) {
        throw new IllegalStateException("La entrega esta marcada como no recibida y debe volver a pendiente antes de reiniciarse.");
    }

    @Override
    public void confirmarRecepcion(Entrega entrega, List<String> fotos) {
        throw new IllegalStateException("No se puede confirmar recepcion desde NO_RECIBIDA.");
    }

    @Override
    public void marcarNoRecibida(Entrega entrega, String motivo) {
        throw new IllegalStateException("La entrega ya fue marcada como no recibida.");
    }

    @Override
    public void volverAPendiente(Entrega entrega) {
        entrega.limpiarMotivoNoRecibida();
        entrega.cambiarEstado(new Pendiente());
    }

    @Override
    public String getNombre() {
        return "NO_RECIBIDA";
    }
}
