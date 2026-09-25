package donatrack.donaciones.domain.persona;

import javax.persistence.*;

@Entity
@Table(name = "Persona_Humana")
public class PersonaHumana extends Persona {

    @Column(name="Nombre")
    private String nombre;

    @Column(name="Apellido")
    private String apellido;

    // No se persiste la edad, tenemos que persistir una fecha de nacimiento
    // private LocalDate fecha
    @Column(name="Edad")
    private int edad;

    @Column(name="Documento")
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name="Genero")
    private Genero genero;

    //Constructor vacío para Hibernate
    protected PersonaHumana() {
    }

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

    @Override
    public String getDocumento() {
        return documento;
    }

    public Genero getGenero() {
        return genero;
    }
}
