package donatrack.model.persona;

import donatrack.model.contacto.MedioContacto;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.usuario.Usuario;
import donatrack.servicio.ServicioSegmentacion;

import java.util.ArrayList;
import java.util.List;

public abstract class Persona {

    protected String direccion;
    protected List<MedioContacto> contactos = new ArrayList<>();
    protected MedioContacto contactoPredeterminado;
    protected Usuario usuario;

    public void ingresarDonacion(List<Bien> bienes) {
        ServicioSegmentacion servicio = new ServicioSegmentacion();
        List<Donacion> donaciones = servicio.segmentar(bienes, this);
        System.out.println("[DONACION] Se generaron " + donaciones.size()
                + " donacion(es) para " + getNombreDisplay());
    }

    public abstract String getNombreDisplay();

    public void agregarMedioContacto(MedioContacto medio) {
        contactos.add(medio);
        if (contactos.size() == 1) {
            contactoPredeterminado = medio;
        }
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
