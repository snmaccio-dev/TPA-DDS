package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

import java.util.List;

public class AsignacionRealizada implements EstadoDonacion {

    @Override
    public void confirmarDestino(Donacion donacion, Beneficiaria destinatario) {
        throw new IllegalStateException("La donacion ya tiene un destinatario confirmado.");
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        donacion.cambiarEstado(new ListaParaEntregar());
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion aun no fue incluida en una ruta planificada.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La donacion aun no fue trasladada.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion no esta en traslado.");
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
        return "ASIGNACION_REALIZADA";
    }
}
