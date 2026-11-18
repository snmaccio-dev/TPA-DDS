package donatrack.donaciones.repository;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioSubcategorias implements WithSimplePersistenceUnit {

    public Optional<Categoria> buscarCategoriaPorNombre(String nombre) {
        return createQuery(
            "select c from Categoria c where c.nombre = :nombre",
            Categoria.class)
            .setParameter("nombre", nombre)
            .getResultList()
            .stream()
            .findFirst();
    }

    public void guardarCategoria(Categoria categoria) {
        persist(categoria);
    }

    public List<Subcategoria> todas() {
        return createQuery(
            "select s from Subcategoria s",
            Subcategoria.class
        ).getResultList();
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

    public void guardar(Subcategoria subcategoria) {
        persist(subcategoria);
    }
}
