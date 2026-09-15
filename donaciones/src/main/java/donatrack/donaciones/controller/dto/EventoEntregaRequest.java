package donatrack.donaciones.controller.dto;

public record EventoEntregaRequest(
    long donacionId,
    String resultado,
    String fechaHora,
    String patenteCamion,
    String motivo
) {
}