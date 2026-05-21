package donatrack.model.persona;

public class PersonaHumana extends Persona {

    private String nombre;
    private String apellido;
    private int edad;
    private String documento;
    private Genero genero;

    public PersonaHumana(String nombre, String apellido, int edad, String documento, Genero genero) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.documento = documento;
        this.genero = genero;
    }

    @Override
    public String getNombreDisplay() {
        return nombre + " " + apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public int getEdad() {
        return edad;
    }

    public String getDocumento() {
        return documento;
    }

    public Genero getGenero() {
        return genero;
    }
}
