package donatrack.web.vista;

// Ubicacion publica de una entidad beneficiaria (GET /entidades/ubicaciones de Donaciones).
public record UbicacionEntidadVista(long id, String razonSocial, double latitud, double longitud) {
}
