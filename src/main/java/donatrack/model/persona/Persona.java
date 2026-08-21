package donatrack.model.persona;

import donatrack.model.contacto.MedioContacto;
import donatrack.model.usuario.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Persona {

    protected String direccion;
    protected List<MedioContacto> contactos = new ArrayList<>();
    protected MedioContacto contactoPredeterminado;
    protected Usuario usuario;
    protected final List<Rol> roles = new ArrayList<>();

    public abstract String getNombreDisplay();

    public void agregarMedioContacto(MedioContacto medio) {
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
        return roles;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public List<MedioContacto> getContactos() {
        return contactos;
    }

    public MedioContacto getContactoPredeterminado() {
        return contactoPredeterminado;
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
