package donatrack.repositorio;

import donatrack.model.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Singleton — unica instancia de almacen de donaciones en memoria
public class RepositorioDonaciones {

    private static RepositorioDonaciones instancia;
    private final List<Donacion> donaciones = new ArrayList<>();

    private RepositorioDonaciones() {}

    public static RepositorioDonaciones getInstance() {
        if (instancia == null) {
            instancia = new RepositorioDonaciones();
        }
        return instancia;
    }

    public void guardar(Donacion donacion) {
        donaciones.add(donacion);
    }

    public List<Donacion> todas() {
        return new ArrayList<>(donaciones);
    }

    public Optional<Donacion> buscarPorId(Long id) {
        return donaciones.stream()
            .filter(d -> d.getId().equals(id))
            .findFirst();
    }

    public void eliminar(Long id) {
        donaciones.removeIf(d -> d.getId().equals(id));
    }

    public int cantidad() {
        return donaciones.size();
    }
}
