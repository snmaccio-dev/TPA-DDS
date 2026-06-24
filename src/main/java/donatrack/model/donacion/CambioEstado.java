package donatrack.model.donacion.estado;

import java.time.LocalDateTime;

public class CambioEstado {

  private String estadoAnterior;
  private String estadoNuevo;
  private LocalDateTime fecha;

  public CambioEstado(String estadoAnterior, String estadoNuevo) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.fecha = LocalDateTime.now();
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
}