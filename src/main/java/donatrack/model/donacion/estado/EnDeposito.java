package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class EnDeposito implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        donacion.cambiarEstado(new AsignacionRealizada());
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("No se puede planificar ruta: la donacion no fue asignada aun.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("No se puede iniciar traslado desde EN_DEPOSITO.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("No se puede confirmar entrega desde EN_DEPOSITO.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        throw new IllegalStateException("No se puede registrar entrega fallida desde EN_DEPOSITO.");
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
