package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.donacion.Donacion;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("DONANTE")
public class Donante extends Rol {

    @Column(name = "ultima_interaccion")
    private LocalDate ultimaInteraccion;

    @Column(name = "fecha_ultimo_aviso_inactividad")
    private LocalDate fechaUltimoAvisoInactividad;

    @OneToMany(mappedBy = "donante")
    private List<Donacion> donacionesRealizadas = new ArrayList<>();

    protected Donante() {
    }

    public Donante(Persona persona) {
        super(persona);
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
