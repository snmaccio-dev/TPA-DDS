package donatrack.logistica.service;

public class RecursoInexistenteException extends RuntimeException {

  public RecursoInexistenteException(String mensaje) {
    super(mensaje);
  }
}
