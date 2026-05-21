package donatrack.notificacion;

import donatrack.model.contacto.MedioContacto;
import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Persona;

public class ServicioNotificaciones {

    public void notificar(Persona destinatario, String mensaje) {
        MedioContacto medio = destinatario.getContactoPredeterminado();
        if (medio == null) {
            System.out.println("[NOTIFICACION] Sin medio de contacto para " + destinatario.getNombreDisplay());
            return;
        }
        Notificador notificador = resolverNotificador(medio.getTipo());
        notificador.notificar(medio.getValor(), mensaje);
    }

    public void notificarPorMedio(String valor, TipoContacto tipo, String mensaje) {
        Notificador notificador = resolverNotificador(tipo);
        notificador.notificar(valor, mensaje);
    }

    private Notificador resolverNotificador(TipoContacto tipo) {
        return switch (tipo) {
            case EMAIL    -> new NotificadorEmail();
            case TELEFONO -> new NotificadorSMS();
            case WHATSAPP -> new NotificadorWhatsApp();
        };
    }
}
