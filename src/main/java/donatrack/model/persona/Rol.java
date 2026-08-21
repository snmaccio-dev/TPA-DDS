package donatrack.model.persona;

public abstract class Rol {

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
