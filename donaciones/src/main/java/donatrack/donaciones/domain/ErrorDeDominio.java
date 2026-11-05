package donatrack.donaciones.domain;

public abstract class ErrorDeDominio extends RuntimeException {

  protected ErrorDeDominio(String mensaje) {
    super(mensaje);
  }
}
