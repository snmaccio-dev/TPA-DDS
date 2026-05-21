package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class EnTraslado implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("No se puede asignar: la donacion ya esta en traslado.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("No se puede planificar ruta: la donacion ya esta en traslado.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("El traslado ya fue iniciado.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        donacion.cambiarEstado(new Entregada());
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        System.out.println("[TRAZABILIDAD] Entrega fallida. Justificacion: " + justificacion);
        donacion.cambiarEstado(new EntregaFallida());
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
