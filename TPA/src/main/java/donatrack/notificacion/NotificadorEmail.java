package donatrack.notificacion;

public class NotificadorEmail implements Notificador {

    @Override
    public void notificar(String destinatario, String mensaje) {
        System.out.println("[EMAIL SIMULADO] Para: " + destinatario + " | Mensaje: " + mensaje);
    }
}
