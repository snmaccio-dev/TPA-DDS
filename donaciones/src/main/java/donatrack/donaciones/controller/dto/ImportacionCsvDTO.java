package donatrack.donaciones.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import donatrack.donaciones.contrato.Fechas;
import donatrack.donaciones.domain.importacion.ErrorDeImportacion;
import donatrack.donaciones.domain.importacion.ImportacionCsv;

import java.util.List;

public record ImportacionCsvDTO(
    Long id,
    String archivo,
    String estado,
    String fechaInicio,
    String fechaFin,
    String motivoFallo,
    int filasLeidas,
    int creados,
    int actualizados,
    int cuentasCreadas,
    int cuentasVinculadas,
    int filasConError,
    int erroresNoGuardados,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<ErrorFilaDTO> errores
) {

  public record ErrorFilaDTO(int fila, String mensaje) {

    static ErrorFilaDTO desde(ErrorDeImportacion error) {
      return new ErrorFilaDTO(error.getNumeroFila(), error.getMensaje());
    }
  }

  public static ImportacionCsvDTO conErrores(ImportacionCsv importacion) {
    return desde(importacion,
        importacion.getErrores().stream().map(ErrorFilaDTO::desde).toList());
  }

  public static List<ImportacionCsvDTO> resumenes(List<ImportacionCsv> importaciones) {
    return importaciones.stream().map(i -> desde(i, null)).toList();
  }

  private static ImportacionCsvDTO desde(ImportacionCsv i, List<ErrorFilaDTO> errores) {
    return new ImportacionCsvDTO(
        i.getId(),
        i.getNombreArchivo(),
        i.getEstado().name(),
        i.getFechaInicio() == null ? null : Fechas.aTexto(i.getFechaInicio()),
        i.getFechaFin() == null ? null : Fechas.aTexto(i.getFechaFin()),
        i.getMotivoFallo(),
        i.getFilasLeidas(),
        i.getCreados(),
        i.getActualizados(),
        i.getCuentasCreadas(),
        i.getCuentasVinculadas(),
        i.getFilasConError(),
        i.getErroresNoGuardados(),
        errores
    );
  }
}
