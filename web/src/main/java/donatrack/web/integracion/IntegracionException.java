package donatrack.web.integracion;

public abstract class IntegracionException extends RuntimeException {

  protected IntegracionException(String mensaje) {
    super(mensaje);
  }

  protected IntegracionException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
