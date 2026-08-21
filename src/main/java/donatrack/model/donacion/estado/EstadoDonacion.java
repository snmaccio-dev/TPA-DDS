package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

import java.util.List;

// State — transiciones del ciclo de vida de una Donacion (7 estados de E1)
public interface EstadoDonacion {
    void asignar(Donacion donacion);
    void marcarListaParaEntregar(Donacion donacion);
    void marcarEnTraslado(Donacion donacion);
    void confirmarRecepcion(Donacion donacion, List<String> fotos);
    void marcarEntregaFallida(Donacion donacion, String motivo);
    void marcarEnDeposito(Donacion donacion);
    void vencer(Donacion donacion);
    String getNombre();
}
