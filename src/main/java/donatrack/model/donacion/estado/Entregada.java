package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

import java.util.List;

public class Entregada implements EstadoDonacion {

    @Override
    public void confirmarDestino(Donacion donacion, Beneficiaria destinatario) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public void vencer(Donacion donacion) {
        throw new IllegalStateException("La donacion ya fue entregada.");
    }

    @Override
    public String getNombre() {
        return "ENTREGADA";
    }
}
