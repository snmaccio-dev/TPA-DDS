package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class Vencida implements EstadoDonacion {

    @Override
    public void asignar(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion esta vencida y no puede ser procesada.");
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        throw new IllegalStateException("La donacion esta vencida.");
    }

    @Override
    public void vencer(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta vencida.");
    }

    @Override
    public String getNombre() {
        return "VENCIDA";
    }
}
