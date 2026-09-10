package donatrack.logistica.domain.flota;

public class Chofer {

    private final String nombre;
    private final String apellido;
    private final String documento;
    private final String licencia;

    public Chofer(String nombre, String apellido, String documento, String licencia) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El chofer debe tener un nombre.");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException("El chofer debe tener un apellido.");
        }
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El chofer debe tener un documento.");
        }
        if (licencia == null || licencia.isBlank()) {
            throw new IllegalArgumentException("El chofer debe tener una licencia.");
        }
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.licencia = licencia;
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
}
