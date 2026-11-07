package donatrack.web.integracion;

import java.util.List;

// Cuerpo del POST /donaciones. Con un solo bien alcanza para el formulario de deposito.
public record NuevaDonacion(
    String donanteDocumento,
    String descripcion,
    List<Bien> bienes
) {

  public record Bien(
      String descripcion,
      String subcategoria,
      double cantidad,
      String condicion,
      String fechaVencimiento
  ) {}
}
