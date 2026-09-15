package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.contacto.TipoContacto;

// Strategy — interfaz comun para todos los canales de notificacion
public interface Notificador {
    void notificar(String destino, String mensaje);

    TipoContacto tipoAtendido();
}