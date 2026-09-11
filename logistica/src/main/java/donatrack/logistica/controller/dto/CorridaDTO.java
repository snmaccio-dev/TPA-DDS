package donatrack.logistica.controller.dto;

import java.util.List;

public record CorridaDTO(
    List<String> solicitudes,
    int entregasEsperandoCallback
) {
}
