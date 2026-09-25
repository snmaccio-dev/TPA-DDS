package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;
import javax.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("BENEFICIARIA")
public class Beneficiaria extends Rol {

    @OneToMany(mappedBy = "entidad")
    private List<Necesidad> necesidades = new ArrayList<>();

    @OneToMany(mappedBy = "destinatarioAsignado")
    private List<Donacion> donacionesRecibidas = new ArrayList<>();

    protected Beneficiaria() {
    }

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
        return List.copyOf(necesidades);
    }

    public List<Donacion> getDonacionesRecibidas() {
        return List.copyOf(donacionesRecibidas);
    }
}
