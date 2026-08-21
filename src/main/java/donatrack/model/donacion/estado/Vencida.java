package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class Vencida implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarEnReparto(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarEntregada(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void devolverAlDeposito(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void vencer(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta marcada como vencida.");
    }

    @Override
    public String getNombre() {
        return "VENCIDA";
    }
}
