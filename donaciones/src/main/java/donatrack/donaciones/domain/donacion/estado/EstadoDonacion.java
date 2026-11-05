package donatrack.donaciones.domain.donacion.estado;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "Estado_Donacion")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_clase_estado") // Esto crea una columna extra para diferenciar si es un estado común o uno de entrega fallida
@DiscriminatorValue("COMUN")
public class EstadoDonacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estado_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadosPosiblesDonacion estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    protected EstadoDonacion() {
    }

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

    public Long getId() {
        return id;
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
