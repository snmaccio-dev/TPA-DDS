package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class AsignacionRealizada implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue asignada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        donacion.cambiarEstado(new ListaParaEntregar());
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("Debe planificar ruta antes de iniciar traslado.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("No se puede confirmar entrega desde ASIGNACION_REALIZADA.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        throw new IllegalStateException("No se puede registrar entrega fallida desde ASIGNACION_REALIZADA.");
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "ASIGNACION_REALIZADA";
    }
}
