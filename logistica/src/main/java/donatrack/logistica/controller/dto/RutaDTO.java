package donatrack.logistica.controller.dto;

import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.ruta.RutaReparto;

import java.util.List;

public record RutaDTO(
    long id,
    String patenteCamion,
    String estado,
    String chofer,
    String fechaHoraInicio,
    double porcentajeAvance,
    List<DestinoDTO> destinos
) {

  public static RutaDTO desde(RutaReparto ruta) {
    return new RutaDTO(
        ruta.getId(),
        ruta.getCamion().getPatente(),
        ruta.getEstado().getNombre(),
        nombreDe(ruta.getChofer()),
        Fechas.aTexto(ruta.getFechaHoraInicio()),
        ruta.getPorcentajeAvance(),
        ruta.getDestinos().stream().map(DestinoDTO::desde).toList()
    );
  }

  public static List<RutaDTO> desde(List<RutaReparto> rutas) {
    return rutas.stream().map(RutaDTO::desde).toList();
  }

  private static String nombreDe(Chofer chofer) {
    return chofer == null ? null : chofer.getNombre() + " " + chofer.getApellido();
  }
}
