package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class EnDeposito implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        donacion.cambiarEstado(new AsignacionRealizada());
    }

    @Override
    public void marcarEnReparto(Donacion donacion) {
        throw new IllegalStateException("No se puede marcar en reparto: la donacion no fue asignada aun.");
    }

    @Override
    public void marcarEntregada(Donacion donacion) {
        throw new IllegalStateException("No se puede marcar como entregada desde EN_DEPOSITO.");
    }

    @Override
    public void devolverAlDeposito(Donacion donacion) {
        throw new IllegalStateException("La donacion ya se encuentra en el deposito.");
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "EN_DEPOSITO";
    }
}
