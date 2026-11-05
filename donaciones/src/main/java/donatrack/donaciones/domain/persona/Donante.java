package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.donacion.Donacion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donante")
public class Donante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donante_id")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "ultima_interaccion")
    private LocalDate ultimaInteraccion;

    @Column(name = "fecha_ultimo_aviso_inactividad")
    private LocalDate fechaUltimoAvisoInactividad;

    @OneToMany(mappedBy = "donante")
    private List<Donacion> donacionesRealizadas = new ArrayList<>();

    protected Donante() {
    }

    public Donante(Persona persona) {
        if (persona == null) {
            throw new DatosInvalidosException("El rol debe estar asociado a una persona.");
        }
        this.persona = persona;
        this.ultimaInteraccion = LocalDate.now();
    }

    public void registrarDonacion(Donacion donacion) {
        donacionesRealizadas.add(donacion);
        registrarInteraccion();
    }

    // Por ahora la unica accion que cuenta como interaccion es registrar una donacion.
    public void registrarInteraccion() {
        this.ultimaInteraccion = LocalDate.now();
    }

    public void registrarAvisoDeInactividad() {
        this.fechaUltimoAvisoInactividad = LocalDate.now();
    }

    public boolean requiereAvisoDeInactividad(LocalDate limite) {
        return ultimaInteraccion.isBefore(limite)
            && (fechaUltimoAvisoInactividad == null
                || fechaUltimoAvisoInactividad.isBefore(ultimaInteraccion));
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public LocalDate getUltimaInteraccion() {
        return ultimaInteraccion;
    }

    public LocalDate getFechaUltimoAvisoInactividad() {
        return fechaUltimoAvisoInactividad;
    }

    public List<Donacion> getDonacionesRealizadas() {
        return List.copyOf(donacionesRealizadas);
    }
}
