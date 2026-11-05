package donatrack.web.integracion;

public record NuevoCamion(
    String patente,
    double capacidadVolumen,
    double altura,
    double capacidadCarga
) {
}
