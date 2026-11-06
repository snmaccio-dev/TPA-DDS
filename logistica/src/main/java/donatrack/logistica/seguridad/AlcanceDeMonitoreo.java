package donatrack.logistica.seguridad;

import java.util.List;

public record AlcanceDeMonitoreo(
    Long beneficiariaId,
    Long donanteId,
    List<Long> donacionIds
) {

  public AlcanceDeMonitoreo {
    donacionIds = donacionIds == null ? List.of() : List.copyOf(donacionIds);
  }

  public static AlcanceDeMonitoreo todas() {
    return new AlcanceDeMonitoreo(null, null, List.of());
  }

  public static AlcanceDeMonitoreo deBeneficiaria(long beneficiariaId) {
    return new AlcanceDeMonitoreo(beneficiariaId, null, List.of());
  }

  public static AlcanceDeMonitoreo deDonante(long donanteId) {
    return new AlcanceDeMonitoreo(null, donanteId, List.of());
  }

  public static AlcanceDeMonitoreo deDonaciones(List<Long> donacionIds) {
    return new AlcanceDeMonitoreo(null, null, donacionIds);
  }

  public boolean esDeBeneficiaria() {
    return beneficiariaId != null;
  }

  public boolean esDeDonante() {
    return donanteId != null;
  }

  public boolean esDeDonaciones() {
    return !donacionIds.isEmpty();
  }
}
