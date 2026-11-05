package donatrack.logistica.domain;

public abstract class ErrorDeDominio extends RuntimeException {

  protected ErrorDeDominio(String mensaje) {
    super(mensaje);
  }
}
