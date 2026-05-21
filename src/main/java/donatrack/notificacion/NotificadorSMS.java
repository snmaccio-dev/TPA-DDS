package donatrack.notificacion;

public class NotificadorSMS implements Notificador {

    @Override
    public void notificar(String destinatario, String mensaje) {
        System.out.println("[SMS SIMULADO] Para: " + destinatario + " | Mensaje: " + mensaje);
    }
}
