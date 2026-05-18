package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

public class EntregaFallida implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("No se puede asignar desde ENTREGA_FALLIDA. Primero debe volver al deposito.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
        throw new IllegalStateException("No se puede planificar ruta desde ENTREGA_FALLIDA.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
        throw new IllegalStateException("No se puede iniciar traslado desde ENTREGA_FALLIDA.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
        throw new IllegalStateException("No se puede confirmar entrega desde ENTREGA_FALLIDA.");
    }

    @Override
    public void fallarEntrega(Donacion donacion, String justificacion) {
        throw new IllegalStateException("La entrega ya fue marcada como fallida.");
    }

    // Volver al deposito es la unica transicion valida desde este estado
    public void devolverAlDeposito(Donacion donacion) {
        donacion.cambiarEstado(new EnDeposito());
    }

    @Override
    public void vencer(Donacion donacion) {
        donacion.cambiarEstado(new Vencida());
    }

    @Override
    public String getNombre() {
        return "ENTREGA_FALLIDA";
    }
}
