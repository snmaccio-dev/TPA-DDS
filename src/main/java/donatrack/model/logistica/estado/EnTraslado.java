package donatrack.model.logistica.estado;

import donatrack.model.logistica.Entrega;

import java.util.List;

public class EnTraslado implements EstadoEntrega {

    @Override
    public void iniciarTraslado(Entrega entrega) {
        throw new IllegalStateException("El traslado ya fue iniciado.");
    }

    @Override
    public void confirmarRecepcion(Entrega entrega, List<String> fotos) {
        entrega.registrarRecepcion(fotos);
        entrega.cambiarEstado(new Entregada());
    }

    @Override
    public void marcarNoRecibida(Entrega entrega, String motivo) {
        entrega.registrarMotivoNoRecibida(motivo);
        entrega.cambiarEstado(new NoRecibida());
    }

    @Override
    public void volverAPendiente(Entrega entrega) {
        throw new IllegalStateException("La entrega esta en traslado y no puede volver a pendiente directamente.");
    }

    @Override
    public String getNombre() {
        return "EN_TRASLADO";
    }
}
