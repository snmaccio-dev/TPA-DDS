package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Persona_Juridica", indexes = @Index(name = "idx_persona_juridica_cuit", columnList = "CUIT"))
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

    // No valida el formato: lo usan los tests y la carga de datos. Las entradas del sistema
    // (registro, alta por administracion, importacion) crean la persona con nueva(...).
    public PersonaJuridica(String cuit, String razonSocial, TipoOrganizacion tipo, String rubro) {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new DatosInvalidosException("La razon social es obligatoria.");
        }
        this.cuit = normalizarCuit(cuit);
        this.razonSocial = razonSocial.trim();
        this.tipo = tipo;
        this.rubro = rubro;
    }

    // Deja un CUIT solo con digitos. Mismo criterio que usa el constructor:
    // los repositorios lo aplican sobre el parametro de busqueda para que
    // un "30-12345678-9" encuentre al que se guardo como "30123456789".
    public static String normalizarCuit(String cuit) {
        if (cuit == null) {
            throw new DatosInvalidosException("El CUIT es obligatorio.");
        }
        return cuit.replaceAll("[^0-9]", "");
    }

    public void actualizarRazonSocial(String razonSocial) {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new DatosInvalidosException("La razon social es obligatoria.");
        }
        this.razonSocial = razonSocial.trim();
    }

    // Alta de una persona juridica (donante o entidad beneficiaria): aplica las reglas de
    // ReglasDePersona al CUIT (11 digitos, con o sin guiones; se guarda sin guiones) y a la
    // razon social.
    public static PersonaJuridica nueva(String cuit,
                                        String razonSocial,
                                        TipoOrganizacion tipo,
                                        String rubro) {
        return new PersonaJuridica(
            ReglasDePersona.cuit(cuit), ReglasDePersona.razonSocial(razonSocial), tipo, rubro);
    }

    // Modificacion: la razon social se valida solo si cambia (un registro viejo que no
    // cumple las reglas se puede seguir editando en lo demas).
    public void actualizarDatos(String razonSocial, TipoOrganizacion tipo, String rubro) {
        actualizarRazonSocialValidada(razonSocial);
        this.tipo = tipo;
        this.rubro = rubro == null ? null : rubro.trim();
    }

    public void actualizarRazonSocialValidada(String razonSocial) {
        if (razonSocial != null
            && !ReglasDePersona.colapsarEspacios(razonSocial)
                .equals(ReglasDePersona.colapsarEspacios(this.razonSocial))) {
            ReglasDePersona.razonSocial(razonSocial);
        }
        actualizarRazonSocial(razonSocial);
    }

    // Cambiar los representantes no cambia la cuenta de Keycloak de la entidad: el usuario
    // sigue siendo el mismo aunque su email ya no sea el de ningun representante.
    public void reemplazarRepresentantes(List<Representante> nuevos) {
        if (nuevos == null || nuevos.isEmpty()) {
            throw new DatosInvalidosException(
                "La beneficiaria debe indicar al menos un representante con su correo.");
        }
        if (nuevos.stream().anyMatch(Objects::isNull)) {
            throw new DatosInvalidosException("El representante es obligatorio.");
        }
        Set<String> emailsQueSalen = emailsDeRepresentantes();
        representantes.clear();
        nuevos.forEach(this::agregarRepresentante);
        emailsQueSalen.removeAll(emailsDeRepresentantes());

        contactos.stream()
            .filter(c -> c.getTipo() == TipoContacto.EMAIL)
            .filter(c -> c != contactoPredeterminado)
            .filter(c -> c.getValor() != null
                && emailsQueSalen.contains(c.getValor().trim().toLowerCase()))
            .toList()
            .forEach(this::quitarContacto);
    }

    private Set<String> emailsDeRepresentantes() {
        Set<String> emails = new HashSet<>();
        representantes.forEach(r -> emails.add(r.getEmail()));
        return emails;
    }

    public void agregarRepresentante(Representante representante) {
        if (representante == null) {
            throw new DatosInvalidosException("El representante es obligatorio.");
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
