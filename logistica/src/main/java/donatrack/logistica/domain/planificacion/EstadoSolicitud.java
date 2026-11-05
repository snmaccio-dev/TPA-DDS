package donatrack.logistica.domain.planificacion;

public enum EstadoSolicitud {
  ENVIADA,
  RESPONDIDA;

  public String getNombre() {
    return name();
  }
}
