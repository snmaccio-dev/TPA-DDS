package donatrack.model.persona;

public class Chofer extends Rol {

    public Chofer(PersonaHumana persona) {
        super(persona);
    }

    @Override
    public PersonaHumana getPersona() {
        return (PersonaHumana) super.getPersona();
    }
}
