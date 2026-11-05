package donatrack.web.integracion;

public record ArchivoSubido(
    String nombre,
    String tipoMime,
    byte[] contenido
) {
}
