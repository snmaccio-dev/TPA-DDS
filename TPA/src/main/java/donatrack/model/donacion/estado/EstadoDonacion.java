package donatrack.model.donacion.estado;

import donatrack.model.donacion.Donacion;

// State — define las transiciones posibles del ciclo de vida de una Donacion
public interface EstadoDonacion {
    void asignar(Donacion donacion);
    void planificarRuta(Donacion donacion);
    void iniciarTraslado(Donacion donacion);
    void confirmarEntrega(Donacion donacion);
    void fallarEntrega(Donacion donacion, String justificacion);
    void vencer(Donacion donacion);
    String getNombre();
}
