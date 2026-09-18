package donatrack.logistica.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.domain.flota.Camion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.Optional;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

class RepositorioCamionesTest implements SimplePersistenceTest {

  private final RepositorioCamiones repositorio = RepositorioCamiones.getInstance();

  @Test
  void unCamionGuardadoSeRecuperaConTodosSusDatos() {
    repositorio.guardar(new Camion("AAA111", 30.5, 2.8, 1200.0));

    entityManager().flush();
    entityManager().clear();

    Optional<Camion> recuperado = repositorio.buscar("AAA111");

    assertTrue(recuperado.isPresent());
    assertNotNull(recuperado.get().getId());
    assertEquals(30.5, recuperado.get().getCapacidadVolumen());
    assertEquals(2.8, recuperado.get().getAltura());
    assertEquals(1200.0, recuperado.get().getCapacidadCarga());
  }

  @Test
  void laPatenteEsUnicaEnLaBase() {
    repositorio.guardar(new Camion("BBB222", 10.0, 2.0, 500.0));

    assertThrows(
        PersistenceException.class,
        () -> repositorio.guardar(new Camion("BBB222", 20.0, 3.0, 900.0))
    );
  }
}
