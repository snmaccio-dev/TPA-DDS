package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;

import java.util.List;

public record FiltroDonaciones(
    EstadosPosiblesDonacion estado,
    String categoria,
    List<String> subcategorias
) {
}
