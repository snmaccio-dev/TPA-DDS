package donatrack.logistica.controller.dto;

public record ActualizarCamionRequest(
    double capacidadVolumen,
    double altura,
    double capacidadCarga
) {
}
