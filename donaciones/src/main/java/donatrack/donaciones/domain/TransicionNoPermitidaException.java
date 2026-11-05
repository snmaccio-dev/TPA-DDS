package donatrack.donaciones.domain;

public class TransicionNoPermitidaException extends ErrorDeDominio {

  public TransicionNoPermitidaException(String mensaje) {
    super(mensaje);
  }
}
