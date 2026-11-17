package donatrack.donaciones.service;

public record DatosBien(
    String descripcion,
    String subcategoria,
    double cantidad,
    String condicion,
    String fechaVencimiento
) {
}
