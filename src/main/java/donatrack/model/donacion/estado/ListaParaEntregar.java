package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class ListaParaEntregar implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta lista para entregar.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta lista para entregar.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        donacion.cambiarEstado(new EnTraslado());
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La donacion aun no fue trasladada.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion aun no fue trasladada.");
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        throw new IllegalStateException("Solo se marca en deposito desde ENTREGA_FALLIDA.");
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
