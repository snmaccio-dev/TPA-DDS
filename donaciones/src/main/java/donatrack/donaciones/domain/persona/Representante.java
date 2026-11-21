package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Representante {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    protected Representante() {
    }

    public Representante(String nombre, String apellido, String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new DatosInvalidosException("El correo del representante es obligatorio.");
        }
        this.email = email.trim().toLowerCase();
        this.nombre = normalizarOpcional(nombre);
        this.apellido = normalizarOpcional(apellido);
    }

    public Representante(String email) {
        this(null, null, email);
    }

    // Representante nuevo que entra al sistema: nombre, apellido y correo con las reglas de
    // ReglasDePersona. El constructor solo normaliza, para poder comparar con los ya
    // cargados (que pueden no cumplir las reglas) sin rechazarlos.
    public static Representante nuevo(String nombre, String apellido, String email) {
        Representante representante = new Representante(nombre, apellido, email);
        ReglasDePersona.representante(representante);
        return representante;
    }

    private static String normalizarOpcional(String valor) {
        String limpio = ReglasDePersona.colapsarEspacios(valor);
        return limpio.isEmpty() ? null : limpio;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getNombreCompleto() {
        if (nombre != null && apellido != null) {
            return nombre + " " + apellido;
        }
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Representante)) return false;
        Representante that = (Representante) o;
        return Objects.equals(email, that.email)
            && Objects.equals(nombre, that.nombre)
            && Objects.equals(apellido, that.apellido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, nombre, apellido);
    }
}
