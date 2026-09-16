package donatrack.donaciones.domain.persona;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Persona_Juridica")
public class PersonaJuridica extends Persona {

    private final String cuit;
    private String razonSocial;
    private TipoOrganizacion tipo;
    private String rubro;
    private List<PersonaHumana> representantes = new ArrayList<>();

    public PersonaJuridica(String cuit, String razonSocial, TipoOrganizacion tipo, String rubro) {
        if (cuit == null) {
            throw new IllegalArgumentException("El CUIT es obligatorio.");
        }
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalArgumentException("La razon social es obligatoria.");
        }
        this.cuit = cuit.replaceAll("[^0-9]", "");
        this.razonSocial = razonSocial.trim();
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

    @Override
    public String getDocumento() {
        return cuit;
    }

    public String getCuit() {
        return cuit;
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
