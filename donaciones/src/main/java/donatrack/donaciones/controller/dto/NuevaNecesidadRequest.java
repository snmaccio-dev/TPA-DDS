package donatrack.donaciones.controller.dto;

public record NuevaNecesidadRequest(
    String cuitBeneficiaria,
    String descripcion,
    int cantidad,
    String subcategoria,
    // "EXTRAORDINARIA" | "RECURRENTE"
    String tipo,
    // Requerido si tipo == RECURRENTE. Valores: SEMANAL, MENSUAL, etc.
    String periodo
) {
}
