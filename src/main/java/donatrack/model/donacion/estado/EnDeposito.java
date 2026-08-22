package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

import java.util.List;

public class EnDeposito implements EstadoDonacion {

    @Override
    public void confirmarDestino(Donacion donacion, Beneficiaria destinatario) {
        if (destinatario == null) {
            throw new IllegalArgumentException(
                "Debe especificarse un destinatario para confirmar el destino."
            );
        }
        donacion.registrarDestinatarioConfirmado(destinatario);
        donacion.cambiarEstado(new AsignacionRealizada());
    }

    @Override
    public void marcarListaParaEntregar(Donacion donacion) {
        throw new IllegalStateException("Solo se marca LISTA_PARA_ENTREGAR desde ASIGNACION_REALIZADA.");
    }

    @Override
    public void marcarEnTraslado(Donacion donacion) {
        throw new IllegalStateException("La donacion no esta lista para entregar.");
    }

    @Override
    public void confirmarRecepcion(Donacion donacion, List<String> fotos) {
        throw new IllegalStateException("La donacion no fue asignada aun.");
    }

    @Override
    public void marcarEntregaFallida(Donacion donacion, String motivo) {
        throw new IllegalStateException("La donacion no esta en traslado.");
    }

    @Override
    public void marcarEnDeposito(Donacion donacion) {
        throw new IllegalStateException("La donacion ya esta en el deposito.");
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
