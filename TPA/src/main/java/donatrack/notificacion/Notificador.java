package donatrack.notificacion;

// Strategy — interfaz comun para todos los canales de notificacion
public interface Notificador {
    void notificar(String destinatario, String mensaje);
}
