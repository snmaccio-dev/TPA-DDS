package donatrack.donaciones.controller.dto;

import java.util.List;

public record EventoRutasPlanificadasRequest(long rutaId, List<Long> donacionIds) {
}