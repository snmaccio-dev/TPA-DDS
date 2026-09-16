package donatrack.donaciones.domain.donacion.estado;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("FALLIDA") // Le dice a la base de datos cómo identificar a esta clase hija en la tabla única
public class EstadoEntregaFallida extends EstadoDonacion {

    @Column(name = "justificacion")
    private String justificacion;

    protected EstadoEntregaFallida() {
    }

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