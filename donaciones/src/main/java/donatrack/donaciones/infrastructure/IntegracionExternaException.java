package donatrack.donaciones.infrastructure;

public class IntegracionExternaException extends RuntimeException {

    public IntegracionExternaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
