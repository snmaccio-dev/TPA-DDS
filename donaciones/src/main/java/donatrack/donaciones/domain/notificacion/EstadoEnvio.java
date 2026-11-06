package donatrack.donaciones.domain.notificacion;

public enum EstadoEnvio {
    // Se envio por el canal externo (email, SMS, WhatsApp) con exito.
    ENVIADA,
    // El canal fallo o faltaba algo para enviar (contacto, notificador).
    FALLIDA,
    // Notificacion que vive solo adentro de la plataforma (por ejemplo, las
    // que verian los administradores en su lista). No tiene canal externo
    // asociado.
    NO_APLICA
}
