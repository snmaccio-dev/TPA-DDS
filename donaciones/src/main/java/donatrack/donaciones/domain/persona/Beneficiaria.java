package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;

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
@Table(name = "beneficiaria")
public class Beneficiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiaria_id")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "persona_juridica_id", nullable = false, unique = true)
    private PersonaJuridica persona;

    @OneToMany(mappedBy = "entidad")
    private List<Necesidad> necesidades = new ArrayList<>();

    @OneToMany(mappedBy = "destinatarioAsignado")
    private List<Donacion> donacionesRecibidas = new ArrayList<>();

    protected Beneficiaria() {
    }

    public Beneficiaria(PersonaJuridica persona) {
        if (persona == null) {
            throw new DatosInvalidosException("El rol debe estar asociado a una persona.");
        }
        this.persona = persona;
    }

    public Long getId() {
        return id;
    }

    public PersonaJuridica getPersona() {
        return persona;
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