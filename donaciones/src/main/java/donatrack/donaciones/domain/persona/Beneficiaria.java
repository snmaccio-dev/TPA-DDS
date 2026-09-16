package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Entidad_Beneficiaria")
public class Beneficiaria extends Rol {

    @OneToMany(mappedBy = "entidad")
    private final List<Necesidad> necesidades = new ArrayList<>();

    // me olvide esta parte perdon
    private final List<Donacion> donacionesRecibidas = new ArrayList<>();

    public Beneficiaria(PersonaJuridica persona) {
        super(persona);
    }

    @Override
    public PersonaJuridica getPersona() {
        return (PersonaJuridica) super.getPersona();
    }

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

    public void registrarDonacionRecibida(Donacion donacion) {
        donacionesRecibidas.add(donacion);
    }

    public long getCantidadDonacionesUltimoTrimestre() {
        LocalDate limite = LocalDate.now().minusMonths(3);
        return donacionesRecibidas.stream()
            .filter(donacion -> donacion.getFechaHoraEntrega() != null)
            .filter(donacion -> !donacion.getFechaHoraEntrega().toLocalDate().isBefore(limite))
            .count();
    }

    public List<Necesidad> getNecesidades() {
        return necesidades;
    }

    public List<Donacion> getDonacionesRecibidas() {
        return donacionesRecibidas;
    }
}
