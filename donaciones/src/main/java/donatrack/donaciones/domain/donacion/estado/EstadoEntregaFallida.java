package donatrack.donaciones.domain.donacion.estado;

import java.time.LocalDateTime;

public class EstadoEntregaFallida extends EstadoDonacion {

    private final String justificacion;

    public EstadoEntregaFallida(String justificacion) {
        super(EstadosPosiblesDonacion.ENTREGA_FALLIDA);
        this.justificacion = justificacion;
    }

    public EstadoEntregaFallida(LocalDateTime fechaInicio, String justificacion) {
        super(EstadosPosiblesDonacion.ENTREGA_FALLIDA, fechaInicio);
        this.justificacion = justificacion;
    }

    public String getJustificacion() {
        return justificacion;
    }
}