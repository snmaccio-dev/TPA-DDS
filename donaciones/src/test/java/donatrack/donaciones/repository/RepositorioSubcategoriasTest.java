package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositorioSubcategoriasTest implements SimplePersistenceTest {

  private final RepositorioSubcategorias repositorio = new RepositorioSubcategorias();

  @Test
  void unaSubcategoriaSeRecuperaPorNombreConSuUnidadYCategoria() {
    Categoria alimentos = new Categoria("Alimentos");
    entityManager().persist(alimentos);
    repositorio.guardar(new Subcategoria("Arroz", alimentos, Unidades.KILOGRAMOS));

    entityManager().flush();
    entityManager().clear();

    Optional<Subcategoria> recuperada = repositorio.buscarPorNombre("Arroz");

    assertTrue(recuperada.isPresent());
    assertEquals("Arroz", recuperada.get().getNombre());
    assertEquals(Unidades.KILOGRAMOS, recuperada.get().getUnidades());
    assertEquals("Alimentos", recuperada.get().getCategoria().getNombre());
  }

  @Test
  void unNombreInexistenteDevuelveVacio() {
    Optional<Subcategoria> recuperada = repositorio.buscarPorNombre("No existe");

    assertTrue(recuperada.isEmpty());
  }

  @Test
  void todasDevuelveLasSubcategoriasGuardadas() {
    Categoria mobiliario = new Categoria("Mobiliario");
    entityManager().persist(mobiliario);
    repositorio.guardar(new Subcategoria("Sillas", mobiliario, Unidades.UNIDADES));
    repositorio.guardar(new Subcategoria("Mesas", mobiliario, Unidades.UNIDADES));

    entityManager().flush();
    entityManager().clear();

    List<Subcategoria> todas = repositorio.todas();

    assertEquals(2, todas.size());
    assertTrue(todas.stream().anyMatch(s -> "Sillas".equals(s.getNombre())));
    assertTrue(todas.stream().anyMatch(s -> "Mesas".equals(s.getNombre())));
  }
}
