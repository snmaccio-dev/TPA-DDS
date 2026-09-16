package donatrack.donaciones.domain.persona;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
    private Persona persona; //le saque FINAL

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
}
