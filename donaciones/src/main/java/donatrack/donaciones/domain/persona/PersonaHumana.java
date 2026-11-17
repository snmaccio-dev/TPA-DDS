package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import javax.persistence.*;

@Entity
@Table(name = "Persona_Humana", indexes = @Index(name = "idx_persona_humana_documento", columnList = "Documento"))
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

    public void actualizarDatos(String nombre, String apellido, Integer edad, Genero genero) {
        if (apellido == null || apellido.isBlank()) {
            throw new DatosInvalidosException("El apellido es obligatorio.");
        }
        if (edad == null || edad < 0) {
            throw new DatosInvalidosException("La edad tiene que ser mayor o igual a 0.");
        }
        actualizarNombre(nombre, apellido);
        this.edad = edad;
        this.genero = genero;
    }

    public void actualizarNombre(String nombre, String apellido) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre es obligatorio.");
        }
        this.nombre = nombre.trim();
        this.apellido = apellido == null ? "" : apellido.trim();
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
