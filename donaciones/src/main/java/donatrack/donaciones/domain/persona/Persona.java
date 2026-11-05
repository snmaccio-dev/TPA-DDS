package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.usuario.Usuario;
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

    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL)
    protected List<MedioContacto> contactos = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contacto_predeterminado_id")
    protected MedioContacto contactoPredeterminado;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="usuario_id")
    protected Usuario usuario;

    @OneToMany(mappedBy = "persona")
    protected List<Rol> roles = new ArrayList<>();

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

    void registrarRol(Rol rol) {
        boolean yaTiene = roles.stream()
            .anyMatch(existente -> existente.getClass().equals(rol.getClass()));
        if (yaTiene) {
            throw new IllegalStateException(
                "La persona ya tiene un rol " + rol.getClass().getSimpleName() + " asignado."
            );
        }
        roles.add(rol);
    }

    public <T extends Rol> Optional<T> comoRol(Class<T> tipo) {
        return roles.stream()
            .filter(tipo::isInstance)
            .map(tipo::cast)
            .findFirst();
    }

    public boolean tieneRol(Class<? extends Rol> tipo) {
        return roles.stream().anyMatch(tipo::isInstance);
    }

    public List<Rol> getRoles() {
        return List.copyOf(roles);
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
