package donatrack.model.persona;

import java.util.ArrayList;
import java.util.List;

public class PersonaJuridica extends Persona {

    private String razonSocial;
    private TipoOrganizacion tipo;
    private String rubro;
    private List<PersonaHumana> representantes = new ArrayList<>();

    public PersonaJuridica(String razonSocial, TipoOrganizacion tipo, String rubro) {
        this.razonSocial = razonSocial;
        this.tipo = tipo;
        this.rubro = rubro;
    }

    public void agregarRepresentante(PersonaHumana representante) {
        representantes.add(representante);
    }

    @Override
    public String getNombreDisplay() {
        return razonSocial;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public TipoOrganizacion getTipo() {
        return tipo;
    }

    public String getRubro() {
        return rubro;
    }

    public List<PersonaHumana> getRepresentantes() {
        return representantes;
    }
}
