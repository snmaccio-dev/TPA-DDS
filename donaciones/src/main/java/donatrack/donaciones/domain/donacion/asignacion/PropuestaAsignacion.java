package donatrack.donaciones.domain.donacion.asignacion;

import donatrack.donaciones.domain.persona.Beneficiaria;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Propuesta_Asignacion")
public class PropuestaAsignacion {

    //Clave primaria propia para la tabla de propuestas
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "propuesta_id")
    private Long id;

    @Column(name = "donacion_id")
    private final long donacionId;

    // Relación de muchos a muchos con las entidades candidatas
    @ManyToMany
    @JoinTable(
        name = "candidatas_por_propuesta",
        joinColumns = @JoinColumn(name = "propuesta_id"),
        inverseJoinColumns = @JoinColumn(name = "beneficiaria_id")
    )
    private final List<Beneficiaria> candidatas;

    @Column(name = "fecha_generacion")
    private final LocalDateTime fechaGeneracion;

    @Column(name = "hubo_coincidencias")
    private final boolean huboCoincidencias;

    protected PropuestaAsignacion() {
    }

    public PropuestaAsignacion(long donacionId,
                               List<Beneficiaria> candidatas,
                               LocalDateTime fechaGeneracion,
                               boolean huboCoincidencias) {
        this.donacionId = donacionId;
        // Evitamos List.copyOf() en constructores JPA para que Hibernate pueda manejar sus propias colecciones
        this.candidatas = new ArrayList<>(candidatas);
        this.fechaGeneracion = fechaGeneracion;
        this.huboCoincidencias = huboCoincidencias;
    }

    public long getDonacionId() {
        return donacionId;
    }

    public List<Beneficiaria> getCandidatas() {
        return List.copyOf(candidatas);
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public boolean huboCoincidencias() {
        return huboCoincidencias;
    }
}