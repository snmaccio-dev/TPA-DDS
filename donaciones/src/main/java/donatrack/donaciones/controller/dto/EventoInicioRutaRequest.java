package donatrack.donaciones.controller.dto;

import java.util.List;

public record EventoInicioRutaRequest(long rutaId, List<Long> donacionIds, String linkMapa) {
}