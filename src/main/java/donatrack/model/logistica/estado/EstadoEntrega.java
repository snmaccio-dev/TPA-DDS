package donatrack.model.logistica.estado;

import donatrack.model.logistica.Entrega;

import java.util.List;

// State — define las transiciones posibles del ciclo de vida de una Entrega
public interface EstadoEntrega {
    void iniciarTraslado(Entrega entrega);
    void confirmarRecepcion(Entrega entrega, List<String> fotos);
    void marcarNoRecibida(Entrega entrega, String motivo);
    void volverAPendiente(Entrega entrega);
    String getNombre();
}
