package donatrack.donaciones.controller.dto;

import donatrack.donaciones.domain.donacion.Bien;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record BienDTO(
    String descripcion,
    String subcategoria,
    double cantidad,
    String unidad,
    String condicion,
    String fechaVencimiento
) {

  public static BienDTO desde(Bien bien) {
    return new BienDTO(
        bien.getDescripcion(),
        bien.getSubcategoria().getNombre(),
        bien.getCantidad(),
        bien.getUnidades().name(),
        bien.getCondicion() == null ? null : bien.getCondicion().name(),
        aTexto(bien.getFechaVencimiento())
    );
  }

  private static String aTexto(LocalDate fecha) {
    return fecha == null ? null : fecha.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}