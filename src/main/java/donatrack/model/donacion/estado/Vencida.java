package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class Vencida implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
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
