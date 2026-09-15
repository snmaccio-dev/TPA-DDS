package donatrack.donaciones.controller.dto;

import java.util.List;

public record NuevaDonacionRequest(
    String donanteDocumento,
    String descripcion,
    List<BienRequest> bienes
) {

  public record BienRequest(
      String descripcion,
      String subcategoria,
      double cantidad,
      String condicion,
      String fechaVencimiento
  ) {}
}