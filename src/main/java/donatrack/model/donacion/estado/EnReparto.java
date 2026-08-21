package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class EnReparto implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta comprometida en reparto.");
    }

    @Override
    public void marcarEnReparto(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta en reparto.");
    }

    @Override
    public void marcarEntregada(Donacion donacion) {
        donacion.cambiarEstado(new Entregada());
    }

    @Override
    public void devolverAlDeposito(Donacion donacion) {
        donacion.cambiarEstado(new EnDeposito());
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "EN_REPARTO";
    }
}
