package donatrack.logistica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import donatrack.logistica.domain.flota.Chofer;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

class GestorChoferesTest implements SimplePersistenceTest {

  private final GestorChoferes gestor = new GestorChoferes();

  @Test
  void unChoferCreadoQuedaDisponibleParaBuscarPorId() {
    Chofer chofer = gestor.crear(new Chofer("Marta", "Diaz", "31777888", "B1"));

    assertNotNull(chofer.getId());
    assertEquals("Marta", gestor.buscar(chofer.getId()).getNombre());
  }

  @Test
  void noSePuedeCrearDosChoferesConElMismoDocumento() {
    gestor.crear(new Chofer("Jose", "Perez", "25999000", "C2"));

    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> gestor.crear(new Chofer("Otro", "Distinto", "25999000", "D3"))
    );

    assertEquals("Ya existe un chofer con documento 25999000.", error.getMessage());
  }

  @Test
  void buscarUnChoferInexistenteAvisaQueNoExiste() {
    assertThrows(RecursoInexistenteException.class, () -> gestor.buscar(999999L));
  }
}
