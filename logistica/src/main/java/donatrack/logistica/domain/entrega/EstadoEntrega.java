package donatrack.logistica.domain.entrega;

public enum EstadoEntrega {
  PENDIENTE,
  EN_TRASLADO,
  ENTREGADA,
  NO_RECIBIDA;

  public String getNombre() {
    return name();
  }
}
