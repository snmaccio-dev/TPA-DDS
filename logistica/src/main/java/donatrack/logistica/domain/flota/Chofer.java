package donatrack.logistica.domain.flota;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import donatrack.logistica.domain.DatosInvalidosException;

@Entity
@Table(name = "chofer")
public class Chofer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String documento;

    @Column(nullable = false)
    private String licencia;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    protected Chofer() {
    }

    public Chofer(String nombre, String apellido, String documento, String licencia) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El chofer debe tener un nombre.");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new DatosInvalidosException("El chofer debe tener un apellido.");
        }
        if (documento == null || documento.isBlank()) {
            throw new DatosInvalidosException("El chofer debe tener un documento.");
        }
        if (licencia == null || licencia.isBlank()) {
            throw new DatosInvalidosException("El chofer debe tener una licencia.");
        }
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.licencia = licencia;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public String getLicencia() {
        return licencia;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void vincularConCuenta(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new DatosInvalidosException(
                "Debe indicarse la cuenta a vincular con el chofer.");
        }
        this.keycloakId = keycloakId;
    }
}
