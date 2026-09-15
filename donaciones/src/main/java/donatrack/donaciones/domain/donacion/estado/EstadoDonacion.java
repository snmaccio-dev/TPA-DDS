package donatrack.donaciones.domain.donacion.estado;

import java.time.LocalDateTime;

public class EstadoDonacion {

    private final EstadosPosiblesDonacion estado;
    private final LocalDateTime fechaInicio;

    public EstadoDonacion(EstadosPosiblesDonacion estado) {
        this(estado, LocalDateTime.now());
    }

    public EstadoDonacion(EstadosPosiblesDonacion estado, LocalDateTime fechaInicio) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser nula.");
        }
        this.estado = estado;
        this.fechaInicio = fechaInicio;
    }

    public EstadosPosiblesDonacion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public String getNombre() {
        return estado.getNombre();
    }
}