package donatrack.donaciones.domain.persona;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Rol")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_rol") // Agrega una columna para saber si es Administrador o Donante
public abstract class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // Constructor vacío obligatorio para JPA
    protected Rol() {
    }

    protected Rol(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("El rol debe estar asociado a una persona.");
        }
        this.persona = persona;
        persona.registrarRol(this);
    }

    public Persona getPersona() {
        return persona;
    }

    public Long getId() {
        return id;
    }
}
