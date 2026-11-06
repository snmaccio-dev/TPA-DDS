package donatrack.web.vista;

import java.util.List;

public record RutaVista(
    long id,
    String patenteCamion,
    String estado,
    String chofer,
    String fechaHoraInicio,
    double porcentajeAvance,
    List<DestinoVista> destinos
) {

  public RutaVista {
    destinos = destinos == null ? List.of() : List.copyOf(destinos);
  }

  public long getId() {
    return id;
  }

  public String getPatenteCamion() {
    return patenteCamion;
  }

  public List<DestinoVista> getDestinos() {
    return destinos;
  }

  public String getChoferTexto() {
    return chofer == null || chofer.isBlank() ? "Sin asignar" : chofer;
  }

  public String getEstadoTexto() {
    return switch (estado == null ? "" : estado) {
      case "PLANIFICADA" -> "Planificada";
      case "EN_CURSO" -> "En curso";
      case "FINALIZADA" -> "Finalizada";
      default -> "Sin estado";
    };
  }

  public String getEstadoEstilo() {
    return switch (estado == null ? "" : estado) {
      case "FINALIZADA" -> "success";
      case "EN_CURSO" -> "info";
      default -> "warning";
    };
  }

  public boolean isPuedeIniciarse() {
    return "PLANIFICADA".equals(estado);
  }

  public String getAvanceTexto() {
    return Formato.porcentaje(porcentajeAvance);
  }

  public String getInicioTexto() {
    String texto = Formato.fechaHoraCorta(fechaHoraInicio);
    return texto == null ? "No iniciada" : texto;
  }

  public int getCantidadDeDestinos() {
    return destinos.size();
  }

  public int getCantidadDeEntregas() {
    return destinos.stream().mapToInt(DestinoVista::getCantidadDeEntregas).sum();
  }
}
