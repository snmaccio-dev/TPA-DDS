package donatrack.logistica.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.domain.flota.Chofer;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.Optional;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

class RepositorioChoferesTest implements SimplePersistenceTest {

  private final RepositorioChoferes repositorio = RepositorioChoferes.getInstance();

  @Test
  void unChoferGuardadoSeRecuperaPorDocumento() {
    repositorio.guardar(new Chofer("Ana", "Ramirez", "30111222", "B1"));

    entityManager().flush();
    entityManager().clear();

    Optional<Chofer> recuperado = repositorio.buscarPorDocumento("30111222");

    assertTrue(recuperado.isPresent());
    assertNotNull(recuperado.get().getId());
    assertEquals("Ana", recuperado.get().getNombre());
    assertEquals("Ramirez", recuperado.get().getApellido());
    assertEquals("B1", recuperado.get().getLicencia());
  }

  @Test
  void elDocumentoEsUnicoEnLaBase() {
    repositorio.guardar(new Chofer("Luis", "Gomez", "27333444", "C2"));

    assertThrows(
        PersistenceException.class,
        () -> repositorio.guardar(new Chofer("Otro", "Distinto", "27333444", "D3"))
    );
  }
}
