package donatrack.logistica.domain.monitoreo;

import java.time.LocalDateTime;

public record ReporteUbicacion(
    long rutaId,
    String patenteCamion,
    double latitud,
    double longitud,
    double velocidadKmh,
    LocalDateTime momento
) {

  public ReporteUbicacion {
    if (patenteCamion == null || patenteCamion.isBlank()) {
      throw new IllegalArgumentException("El reporte debe indicar la patente del camion.");
    }
    if (latitud < -90 || latitud > 90) {
      throw new IllegalArgumentException("La latitud reportada esta fuera de rango.");
    }
    if (longitud < -180 || longitud > 180) {
      throw new IllegalArgumentException("La longitud reportada esta fuera de rango.");
    }
    if (velocidadKmh < 0) {
      throw new IllegalArgumentException("La velocidad reportada no puede ser negativa.");
    }
    if (momento == null) {
      throw new IllegalArgumentException("El reporte debe indicar el momento de la medicion.");
    }
  }

  public Posicion posicion() {
    return new Posicion(latitud, longitud);
  }
}
