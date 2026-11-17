package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "Persona")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="Direccion")
    protected String direccion;

    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<MedioContacto> contactos = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contacto_predeterminado_id")
    protected MedioContacto contactoPredeterminado;

    public Long getId() {
        return id;
    }

    public abstract String getNombreDisplay();

    public abstract String getDocumento();

    public boolean tieneContactoDeTipo(TipoContacto tipo) {
        return contactos.stream().anyMatch(c -> c.getTipo() == tipo);
    }

    public boolean tieneContacto(TipoContacto tipo, String valor) {
        return contactos.stream()
            .anyMatch(c -> c.getTipo() == tipo && c.getValor().equals(valor));
    }

    public void agregarMedioContacto(MedioContacto medio) {
        medio.setPersona(this);
        contactos.add(medio);
        if (contactos.size() == 1) {
            contactoPredeterminado = medio;
        }
    }

    public void reemplazarContacto(TipoContacto tipo, String valor) {
        if (tipo == null) {
            throw new DatosInvalidosException("El tipo de contacto es obligatorio.");
        }
        List<MedioContacto> delTipo = contactos.stream()
            .filter(c -> c.getTipo() == tipo)
            .toList();
        if (valor == null || valor.isBlank()) {
            delTipo.forEach(this::quitarContacto);
            return;
        }
        if (delTipo.isEmpty()) {
            agregarMedioContacto(new MedioContacto(tipo, valor.trim()));
            return;
        }
        MedioContacto conservado = delTipo.contains(contactoPredeterminado)
            ? contactoPredeterminado
            : delTipo.get(0);
        conservado.setValor(valor.trim());
        delTipo.stream().filter(c -> c != conservado).forEach(this::quitarContacto);
    }

    public void elegirContactoPredeterminado(TipoContacto tipo) {
        if (tipo == null) {
            if (contactoPredeterminado == null || !contactos.contains(contactoPredeterminado)) {
                throw new DatosInvalidosException(
                    "Hay que elegir el contacto predeterminado entre los contactos de la persona.");
            }
            return;
        }
        if (contactoPredeterminado != null
            && contactoPredeterminado.getTipo() == tipo
            && contactos.contains(contactoPredeterminado)) {
            return;
        }
        contactoPredeterminado = contactos.stream()
            .filter(c -> c.getTipo() == tipo)
            .findFirst()
            .orElseThrow(() -> new DatosInvalidosException(
                "El contacto predeterminado " + tipo
                    + " no corresponde a ningun contacto de la persona."));
    }

    void quitarContacto(MedioContacto medio) {
        contactos.remove(medio);
        if (medio == contactoPredeterminado) {
            contactoPredeterminado = null;
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public List<MedioContacto> getContactos() {
        return List.copyOf(contactos);
    }

    public MedioContacto getContactoPredeterminado() {
        return contactoPredeterminado;
    }

    public Optional<MedioContacto> getContactoParaNotificar() {
        if (contactoPredeterminado != null) {
            return Optional.of(contactoPredeterminado);
        }
        return contactos.stream().findFirst();
    }

    public void setContactoPredeterminado(MedioContacto contactoPredeterminado) {
        this.contactoPredeterminado = contactoPredeterminado;
    }

    // Antes de borrar la persona, rompemos la referencia cruzada al
    // contactoPredeterminado. Persona.contacto_predeterminado_id apunta a
    // MedioContacto y MedioContacto.persona_id apunta a Persona, asi que si
    // no la liberamos primero Hibernate puede pifiarle al orden de los
    // delete y la base rechaza por foreign key. No borra el medio: eso lo
    // hace el cascade del @OneToMany contactos.
    public void prepararParaEliminar() {
        this.contactoPredeterminado = null;
    }
}