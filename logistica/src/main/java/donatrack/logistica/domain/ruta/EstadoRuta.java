package donatrack.logistica.domain.ruta;

public enum EstadoRuta {
  PLANIFICADA,
  EN_CURSO,
  FINALIZADA;

  public String getNombre() {
    return name();
  }
}
