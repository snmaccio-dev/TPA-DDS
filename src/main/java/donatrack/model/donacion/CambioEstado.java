package donatrack.model.donacion;

import java.time.LocalDateTime;

public class CambioEstado {

  private final String estadoAnterior;
  private final String estadoNuevo;
  private final LocalDateTime fecha;
  private final String motivo;

  public CambioEstado(String estadoAnterior, String estadoNuevo) {
    this(estadoAnterior, estadoNuevo, null);
  }

  public CambioEstado(String estadoAnterior, String estadoNuevo, String motivo) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.fecha = LocalDateTime.now();
    this.motivo = motivo;
  }

  public String getEstadoAnterior() {
    return estadoAnterior;
  }

  public String getEstadoNuevo() {
    return estadoNuevo;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public String getMotivo() {
    return motivo;
  }
}
