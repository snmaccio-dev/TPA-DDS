package donatrack.web.vista;

import java.util.Comparator;
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

  // Las `cantidad` entregas mas recientes (por fecha de entrega, ISO-8601, descendente).
  // Logistica ya las manda en ese orden; se reordena igual para no depender de eso.
  public static List<EntregaPublicaVista> recientes(List<EntregaPublicaVista> entregas,
                                                    int cantidad) {
    return entregas.stream()
        .sorted(Comparator.comparing(EntregaPublicaVista::fechaHoraEntrega,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(cantidad)
        .toList();
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
