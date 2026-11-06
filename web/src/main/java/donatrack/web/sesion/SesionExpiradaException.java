package donatrack.web.sesion;

public class SesionExpiradaException extends RuntimeException {

  public SesionExpiradaException(String mensaje) {
    super(mensaje);
  }
}
