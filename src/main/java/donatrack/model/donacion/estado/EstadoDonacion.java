package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

// State — define las transiciones posibles del ciclo de vida de una Donacion
public interface EstadoDonacion {
    void asignar(Donacion donacion);
    void marcarEnReparto(Donacion donacion);
    void marcarEntregada(Donacion donacion);
    void devolverAlDeposito(Donacion donacion);
    void vencer(Donacion donacion);
    String getNombre();
}
