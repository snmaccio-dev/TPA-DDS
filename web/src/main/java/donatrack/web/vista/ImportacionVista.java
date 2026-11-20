package donatrack.web.vista;

// Resumen de una importacion de donantes (GET /donantes/importaciones). Los contadores y
// los errores por fila que devuelve Donaciones no se usan en la pantalla.
public record ImportacionVista(
    Long id,
    String archivo,
    String estado,
    String fechaInicio,
    String motivoFallo
) {

  private static final String EN_CURSO = "EN_CURSO";
  private static final String FINALIZADA = "FINALIZADA";
  private static final String FALLIDA = "FALLIDA";

  public Long getId() {
    return id;
  }

  public String getArchivo() {
    return archivo;
  }

  public String getFechaTexto() {
    String texto = Formato.fechaHoraCorta(fechaInicio);
    return texto == null ? "—" : texto;
  }

  public boolean isEnCurso() {
    return EN_CURSO.equals(estado);
  }

  public String getEstadoTexto() {
    if (FINALIZADA.equals(estado)) return "Terminada";
    if (FALLIDA.equals(estado)) return "Fallida";
    return "En curso";
  }

  public String getEstadoClase() {
    if (FINALIZADA.equals(estado)) return "success";
    if (FALLIDA.equals(estado)) return "danger";
    return "info";
  }

  // Solo tiene sentido cuando la importacion fallo.
  public String getMotivoFallo() {
    return FALLIDA.equals(estado) ? motivoFallo : null;
  }
}
