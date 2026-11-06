package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;

import java.util.List;

// Filtros opcionales para listar donaciones. Cualquier campo puede ser null
// (o vacio en subcategorias) para no aplicar esa condicion.
public record FiltroDonaciones(
    EstadosPosiblesDonacion estado,
    String categoria,
    List<String> subcategorias
) {
}
