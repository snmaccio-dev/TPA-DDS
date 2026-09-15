package donatrack.logistica.contrato;

public record EntregaParaPlanificar(
    long entregaId,
    String direccionDestino,
    String descripcion,
    double pesoKg,
    double volumenM3
) {
}
