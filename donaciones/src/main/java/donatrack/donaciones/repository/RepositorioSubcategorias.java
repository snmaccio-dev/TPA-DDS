package donatrack.donaciones.repository;

import donatrack.donaciones.domain.catalogo.Subcategoria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioSubcategorias {

    private final Map<String, Subcategoria> porNombre = new HashMap<>();

    public void guardar(Subcategoria subcategoria) {
        porNombre.put(subcategoria.getNombre(), subcategoria);
    }

    public Optional<Subcategoria> buscarPorNombre(String nombre) {
        return Optional.ofNullable(porNombre.get(nombre));
    }

    public List<Subcategoria> todas() {
        return List.copyOf(porNombre.values());
    }
}