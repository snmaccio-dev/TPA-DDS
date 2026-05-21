package donatrack.notificacion;

public class NotificadorWhatsApp implements Notificador {

    @Override
    public void notificar(String destinatario, String mensaje) {
        System.out.println("[WHATSAPP SIMULADO] Para: " + destinatario + " | Mensaje: " + mensaje);
    }
}
