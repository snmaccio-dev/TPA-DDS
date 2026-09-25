package donatrack.donaciones.domain.contacto;

import donatrack.donaciones.domain.persona.Persona;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "MedioContacto")
public class MedioContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medio_contacto_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoContacto tipo;

    @Column(name = "valor")
    private String valor;

    // Relación bidireccional: un medio de contacto pertenece a una persona
    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // Constructor vacío obligatorio para JPA (Hibernate)
    protected MedioContacto() {
    }

    public MedioContacto(TipoContacto tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public TipoContacto getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    // Nuevos Getters/Setters para la persistencia
    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }
}
