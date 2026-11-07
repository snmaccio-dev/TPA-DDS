package donatrack.web.integracion;

import java.util.List;

// Respuesta de POST /asignaciones/ejecutar. Los nombres coinciden con
// AsignacionesController.EjecutarMatchmakingResponse del modulo donaciones.
public record ResultadoEjecucion(int procesadas, List<String> fallidas) {

  public ResultadoEjecucion {
    fallidas = fallidas == null ? List.of() : List.copyOf(fallidas);
  }

  public int cantidadFallidas() {
    return fallidas.size();
  }
}
