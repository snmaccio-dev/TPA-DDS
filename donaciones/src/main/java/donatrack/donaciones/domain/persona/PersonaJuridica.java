package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Persona_Juridica")
public class PersonaJuridica extends Persona {

    @Column(name="CUIT")
    private String cuit;

    @Column(name="Razon_Social")
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name="Tipo_Organizacion")
    private TipoOrganizacion tipo;
    
    @Column(name="Rubro")
    private String rubro;

    @ElementCollection
    @CollectionTable(
        name = "representante",
        joinColumns = @JoinColumn(name = "persona_juridica_id")
    )
    private List<Representante> representantes = new ArrayList<>();

    //Constructor vacío para Hibernate
    protected PersonaJuridica() {
    }

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

    public void agregarRepresentante(Representante representante) {
        if (representante == null) {
            throw new IllegalArgumentException("El representante es obligatorio.");
        }
        if (representantes.contains(representante)) {
            return;
        }
        representantes.add(representante);
        if (!tieneContacto(TipoContacto.EMAIL, representante.getEmail())) {
            agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, representante.getEmail()));
        }
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

    public List<Representante> getRepresentantes() {
        return List.copyOf(representantes);
    }
}
