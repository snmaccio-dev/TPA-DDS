package donatrack.donaciones.domain.persona;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public abstract class Rol {

    @ManyToOne
    @JoinColumn(name = "persona_id")
    private final Persona persona;

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
