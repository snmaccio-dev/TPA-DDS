package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class Entregada implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void vencer(Donacion donacion) {
        throw new IllegalStateException("No se puede vencer una donacion ya entregada.");
    }

    @Override
    public String getNombre() {
        return "ENTREGADA";
    }
}
