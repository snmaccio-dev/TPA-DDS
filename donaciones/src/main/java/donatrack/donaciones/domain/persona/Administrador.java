package donatrack.donaciones.domain.persona;

public class Administrador extends Rol {

    public Administrador(PersonaHumana persona) {
        super(persona);
    }

    @Override
    public PersonaHumana getPersona() {
        return (PersonaHumana) super.getPersona();
    }
}
