package donatrack.donaciones.infrastructure.notificacion;

import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.notificacion.Notificador;

public class NotificadorEmail implements Notificador {

    @Override
    public void notificar(String destino, String mensaje) {
        System.out.println("[EMAIL SIMULADO] Para: " + destino + " | Mensaje: " + mensaje);
    }

    @Override
    public TipoContacto tipoAtendido() {
        return TipoContacto.EMAIL;
    }
}