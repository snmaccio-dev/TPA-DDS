package donatrack.logistica.domain.integracion;

public class IntegracionExternaException extends RuntimeException {

  public IntegracionExternaException(String mensaje) {
    super(mensaje);
  }

  public IntegracionExternaException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
