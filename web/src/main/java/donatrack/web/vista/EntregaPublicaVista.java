package donatrack.web.vista;

import java.util.List;

public record EntregaPublicaVista(
    long entregaId,
    long donacionId,
    long beneficiariaId,
    String descripcion,
    double cantidad,
    String unidad,
    String fechaHoraEntrega,
    List<String> fotos
) {

  public EntregaPublicaVista {
    fotos = fotos == null ? List.of() : List.copyOf(fotos);
  }

  public long getEntregaId() {
    return entregaId;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getCantidadTexto() {
    return Formato.cantidad(cantidad, unidad);
  }

  public String getFechaHoraEntrega() {
    return Formato.fechaHoraCorta(fechaHoraEntrega);
  }

  public List<String> getFotos() {
    return fotos;
  }

  public boolean getTieneFoto() {
    return !fotos.isEmpty();
  }

  public String getPrimeraFoto() {
    return fotos.isEmpty() ? null : fotos.get(0);
  }
}
