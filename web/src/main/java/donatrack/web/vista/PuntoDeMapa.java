package donatrack.web.vista;

public record PuntoDeMapa(
    double latitud,
    double longitud,
    String titulo,
    String detalle
) {
}
