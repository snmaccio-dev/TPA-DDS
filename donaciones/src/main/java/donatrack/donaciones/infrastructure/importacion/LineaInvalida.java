package donatrack.donaciones.infrastructure.importacion;

public record LineaInvalida(int numero, String mensaje) implements LineaCsv {
}
