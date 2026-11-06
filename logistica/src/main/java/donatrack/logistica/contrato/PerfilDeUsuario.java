package donatrack.logistica.contrato;

public record PerfilDeUsuario(
    String keycloakId,
    String email,
    Long donanteId,
    Long beneficiariaId,
    String nombre
) {

  public boolean esLaBeneficiaria(long id) {
    return beneficiariaId != null && beneficiariaId == id;
  }

  public boolean esElDonante(long id) {
    return donanteId != null && donanteId == id;
  }

  public boolean tienePerfilDeBeneficiaria() {
    return beneficiariaId != null;
  }

  public boolean tienePerfilDeDonante() {
    return donanteId != null;
  }
}
