package donatrack.model.entidad;

import donatrack.model.necesidad.Necesidad;
import donatrack.model.persona.Persona;

import java.util.ArrayList;
import java.util.List;

public class EntidadBeneficiaria extends Persona {

    private List<Necesidad> necesidades = new ArrayList<>();

    public EntidadBeneficiaria(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    private String razonSocial;

    public void registrarNecesidades(Necesidad necesidad) {
        necesidades.add(necesidad);
        System.out.println("[ENTIDAD] Necesidad registrada: " + necesidad.getDescripcion()
                + " (" + necesidad.getCantidad() + " " + necesidad.getSubcategoria() + ")");
    }

    @Override
    public String getNombreDisplay() {
        return razonSocial;
    }

    public List<Necesidad> getNecesidades() {
        return necesidades;
    }

    public String getRazonSocial() {
        return razonSocial;
    }
}
