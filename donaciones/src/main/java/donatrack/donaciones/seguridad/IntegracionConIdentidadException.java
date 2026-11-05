package donatrack.donaciones.seguridad;

// Falla al dialogar con el proveedor de identidad (red caida, 5xx, respuesta
// inesperada). Se mapea a 502 en DonacionesApp. Nunca incluye contrasenas en
// su mensaje: solo las reenviamos al proveedor, nunca las logueamos.
public class IntegracionConIdentidadException extends RuntimeException {

    public IntegracionConIdentidadException(String mensaje) {
        super(mensaje);
    }

    public IntegracionConIdentidadException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}