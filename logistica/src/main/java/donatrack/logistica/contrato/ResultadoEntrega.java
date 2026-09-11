package donatrack.logistica.contrato;

public enum ResultadoEntrega {
  ENTREGADA,
  NO_RECIBIDA;

  public String getNombre() {
    return name();
  }
}
