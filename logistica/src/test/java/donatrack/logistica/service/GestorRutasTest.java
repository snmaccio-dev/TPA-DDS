package donatrack.logistica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.entrega.EstadoEntrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioRutas;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class GestorRutasTest implements SimplePersistenceTest {

  private final GestorRutas gestor = new GestorRutas();
  private final RepositorioRutas repositorio = RepositorioRutas.getInstance();

  private RutaReparto rutaPlanificada(String patente, long donacionId) {
    Camion camion = new Camion(patente, 40.0, 3.0, 2000.0);
    persist(camion);

    Entrega entrega = new Entrega(donacionId, 900L, "Destino 1", "Alimentos", 5, "cajas");
    entrega.registrarMedicion(4.0, 0.2);
    persist(entrega);

    RutaReparto ruta = new RutaReparto(
        camion,
        List.of(new DestinoEntrega(0, "Destino 1", List.of(entrega)))
    );
    persist(ruta);
    commitTransaction();

    return ruta;
  }

  @Test
  void borrarUnaRutaNoIniciadaDevuelveSusEntregasAPendiente() {
    RutaReparto ruta = rutaPlanificada("DEL111", 801L);
    Entrega entrega = ruta.getEntregas().get(0);

    gestor.eliminar(ruta.getId());

    assertTrue(repositorio.buscarPorId(ruta.getId()).isEmpty());
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
    assertNull(entrega.getRuta());
  }

  @Test
  void noSePuedeBorrarUnaRutaYaIniciada() {
    RutaReparto ruta = rutaPlanificada("DEL222", 802L);

    Chofer chofer = new Chofer("Ana", "Ramirez", "29111222", "B1");
    beginTransaction();
    persist(chofer);
    ruta.iniciarRuta(chofer);
    commitTransaction();

    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> gestor.eliminar(ruta.getId())
    );

    assertEquals(
        "La ruta " + ruta.getId() + " ya fue iniciada: no se puede eliminar.",
        error.getMessage()
    );
    assertTrue(repositorio.buscarPorId(ruta.getId()).isPresent());
  }
}
