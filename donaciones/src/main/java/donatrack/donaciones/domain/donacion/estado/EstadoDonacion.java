package donatrack.donaciones.domain.donacion.estado;

public enum EstadoDonacion {
    EN_DEPOSITO,
    ASIGNACION_REALIZADA,
    LISTA_PARA_ENTREGAR,
    EN_TRASLADO,
    ENTREGADA,
    ENTREGA_FALLIDA,
    VENCIDA;

    public String getNombre() {
        return name();
    }
}
