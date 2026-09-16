package donatrack.donaciones.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Usuario {

    @Column(name="nombre")
    private String nombre;

    @Column(name="contrasena")
    private String contrasena;

    public Usuario(String nombre, String contrasena) {
        this.nombre = nombre;
        this.contrasena = contrasena;
    }

    public String getNombre() {
        return nombre;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
