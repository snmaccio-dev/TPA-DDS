package donatrack.donaciones.controller.dto;

import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;

import java.util.List;

public record EstadoHistorialDTO(String estado, String fechaInicio) {

  public static EstadoHistorialDTO desde(EstadoDonacion estado) {
    return new EstadoHistorialDTO(estado.getNombre(), Fechas.aTexto(estado.getFechaInicio()));
  }

  public static List<EstadoHistorialDTO> desde(List<EstadoDonacion> historial) {
    return historial.stream().map(EstadoHistorialDTO::desde).toList();
  }
}