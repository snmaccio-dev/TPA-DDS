package donatrack.donaciones.domain.persona;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Rol {

    protected Administrador() {
    }

    public Administrador(PersonaHumana persona) {
        super(persona);
    }

    @Override
    public PersonaHumana getPersona() {
        return (PersonaHumana) super.getPersona();
    }
}
