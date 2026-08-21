package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class EnTraslado implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion esta en traslado.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue despachada.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta en traslado.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        donacion.registrarRecepcion(fotos);
        donacion.cambiarEstado(new Entregada());
        donacion.getDestinatarioAsignado().registrarDonacionRecibida(donacion);
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        donacion.cambiarEstado(new EntregaFallida(), motivo);
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        throw new IllegalStateException("Solo se marca en deposito desde ENTREGA_FALLIDA.");
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "EN_TRASLADO";
    }
}
