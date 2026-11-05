package donatrack.donaciones.domain;

public class ConflictoDeNegocioException extends ErrorDeDominio {

  public ConflictoDeNegocioException(String mensaje) {
    super(mensaje);
  }
}
