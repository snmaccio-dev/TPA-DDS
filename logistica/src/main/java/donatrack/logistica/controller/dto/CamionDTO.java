package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.SituacionCamion;

import java.util.List;
import java.util.Set;

public record CamionDTO(
    String patente,
    double capacidadVolumen,
    double altura,
    double capacidadCarga,
    SituacionCamion situacion
) {

  public static CamionDTO desde(Camion camion, boolean enRuta) {
    return new CamionDTO(
        camion.getPatente(),
        camion.getCapacidadVolumen(),
        camion.getAltura(),
        camion.getCapacidadCarga(),
        enRuta ? SituacionCamion.EN_RUTA : SituacionCamion.DISPONIBLE
    );
  }

  public static CamionDTO disponible(Camion camion) {
    return desde(camion, false);
  }

  public static List<CamionDTO> desde(List<Camion> camiones, Set<String> patentesEnRuta) {
    return camiones.stream()
        .map(camion -> desde(camion, patentesEnRuta.contains(camion.getPatente())))
        .toList();
  }
}
