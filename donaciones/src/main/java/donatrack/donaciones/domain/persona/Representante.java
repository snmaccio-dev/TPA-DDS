package donatrack.donaciones.domain.persona;

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
            throw new IllegalArgumentException("El correo del representante es obligatorio.");
        }
        this.email = email.trim().toLowerCase();
        this.nombre = normalizarOpcional(nombre);
        this.apellido = normalizarOpcional(apellido);
    }

    public Representante(String email) {
        this(null, null, email);
    }

    private static String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
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
