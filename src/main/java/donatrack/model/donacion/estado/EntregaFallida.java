package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class EntregaFallida implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion tuvo una entrega fallida y espera resolucion.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("Antes de replanificar hay que marcar EN_DEPOSITO.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("Antes de replanificar hay que marcar EN_DEPOSITO.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La entrega esta fallida y espera resolucion.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion ya tuvo una entrega fallida.");
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        donacion.limpiarDestinatario();
        donacion.limpiarCamion();
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
