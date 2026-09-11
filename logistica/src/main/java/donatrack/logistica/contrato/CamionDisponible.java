package donatrack.logistica.contrato;

public record CamionDisponible(
    String patente,
    double capacidadVolumenM3,
    double alturaM,
    double capacidadCargaKg
) {
}
