package donatrack.donaciones.repository;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioSubcategorias implements WithSimplePersistenceUnit {

    public void guardar(Subcategoria subcategoria) {
        persist(subcategoria);
    }

    public Optional<Subcategoria> buscarPorNombre(String nombre) {
        return createQuery(
            "select s from Subcategoria s where s.nombre = :nombre",
            Subcategoria.class)
            .setParameter("nombre", nombre)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<Subcategoria> todas() {
        return createQuery("select s from Subcategoria s", Subcategoria.class).getResultList();
    }
}
