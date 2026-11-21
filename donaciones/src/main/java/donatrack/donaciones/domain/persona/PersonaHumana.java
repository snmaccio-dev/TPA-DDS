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

    // No valida el formato: lo usan los tests y la carga de datos. Las entradas del sistema
    // (registro, alta por administracion) crean la persona con nueva(...).
    public PersonaHumana(String nombre, String apellido, int edad, String documento, Genero genero) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.documento = documento;
        this.genero = genero;
    }

    // Alta de un donante humano: aplica las reglas de ReglasDePersona a nombre, apellido,
    // edad y documento.
    public static PersonaHumana nueva(String nombre,
                                      String apellido,
                                      Integer edad,
                                      String documento,
                                      Genero genero) {
        return new PersonaHumana(
            ReglasDePersona.nombre("El nombre", nombre),
            ReglasDePersona.nombre("El apellido", apellido),
            ReglasDePersona.edad(edad),
            ReglasDePersona.documento(documento),
            genero);
    }

    // Alta desde la importacion CSV, que no trae edad ni genero: la edad queda en 0 (sin
    // dato) y el genero en OTRO, como hasta ahora.
    public static PersonaHumana importada(String nombre, String apellido, String documento) {
        return new PersonaHumana(
            ReglasDePersona.nombre("El nombre", nombre),
            ReglasDePersona.nombre("El apellido", apellido),
            0,
            ReglasDePersona.documento(documento),
            Genero.OTRO);
    }

    // Modificacion: se validan solo los campos que cambian, asi un registro viejo que no
    // cumple las reglas se puede seguir editando en lo demas. Valida todo antes de cambiar
    // nada.
    public void actualizarDatos(String nombre, String apellido, Integer edad, Genero genero) {
        String nuevoNombre = siCambia(this.nombre, nombre, "El nombre");
        String nuevoApellido = siCambia(this.apellido, apellido, "El apellido");
        if (edad == null) {
            throw new DatosInvalidosException("La edad es obligatoria.");
        }
        if (edad != this.edad) {
            ReglasDePersona.edad(edad);
        }
        this.nombre = nuevoNombre;
        this.apellido = nuevoApellido;
        this.edad = edad;
        this.genero = genero;
    }

    // Reemplaza nombre y apellido validando los que cambian (importacion de una persona que
    // ya existe).
    public void actualizarNombreValidado(String nombre, String apellido) {
        String nuevoNombre = siCambia(this.nombre, nombre, "El nombre");
        String nuevoApellido = siCambia(this.apellido, apellido, "El apellido");
        this.nombre = nuevoNombre;
        this.apellido = nuevoApellido;
    }

    private static String siCambia(String actual, String nuevo, String campo) {
        String normalizado = ReglasDePersona.colapsarEspacios(nuevo);
        if (normalizado.equals(ReglasDePersona.colapsarEspacios(actual))) {
            return normalizado;
        }
        return ReglasDePersona.nombre(campo, normalizado);
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
