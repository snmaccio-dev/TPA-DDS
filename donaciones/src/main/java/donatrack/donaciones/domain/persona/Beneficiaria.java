package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;

import javax.persistence.Column;
import javax.persistence.Embedded;
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
import java.util.Objects;
import java.util.Optional;

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

    // Ubicacion de la direccion de la entidad, para el mapa publico de entregas. Es
    // opcional: se carga en el alta (solo administracion) o se geocodifica la primera vez
    // que se le confirma una entrega. Si cambia la direccion, se borra para recalcularla.
    @Embedded
    private Coordenadas ubicacion;

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

    public Optional<Coordenadas> getUbicacion() {
        return Optional.ofNullable(ubicacion);
    }

    public boolean tieneUbicacion() {
        return ubicacion != null;
    }

    public void ubicarEn(Coordenadas coordenadas) {
        this.ubicacion = coordenadas;
    }

    // Se llama despues de modificar los datos de la persona: si la direccion ya no es la
    // que tenia, las coordenadas quedaron viejas y se descartan.
    public void olvidarUbicacionSiCambioLaDireccion(String direccionAnterior) {
        if (!Objects.equals(direccionAnterior, persona.getDireccion())) {
            this.ubicacion = null;
        }
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

    // Regla de compatibilidad de la asignacion: la entidad declaro al menos una necesidad
    // (recurrente o extraordinaria) de la subcategoria del bien donado. La usan el
    // matchmaking (para elegir candidatas) y la confirmacion del destino.
    public boolean tieneNecesidadDe(Subcategoria subcategoria) {
        return necesidades.stream().anyMatch(n -> n.esDeLaSubcategoria(subcategoria));
    }

    // Cantidad total pedida en las necesidades de esa subcategoria (0 si no tiene).
    public int cantidadNecesitadaDe(Subcategoria subcategoria) {
        return necesidades.stream()
            .filter(n -> n.esDeLaSubcategoria(subcategoria))
            .mapToInt(Necesidad::getCantidad)
            .sum();
    }

    public List<Necesidad> getNecesidades() {
        return List.copyOf(necesidades);
    }

    public List<Donacion> getDonacionesRecibidas() {
        return List.copyOf(donacionesRecibidas);
    }
}