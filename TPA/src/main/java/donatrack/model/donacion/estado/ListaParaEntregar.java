package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class ListaParaEntregar implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue asignada y tiene ruta planificada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("La ruta ya fue planificada.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        donacion.cambiarEstado(new EnTraslado());
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("Debe iniciar el traslado antes de confirmar entrega.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        throw new IllegalStateException("No se puede registrar entrega fallida desde LISTA_PARA_ENTREGAR.");
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "LISTA_PARA_ENTREGAR";
    }
}
