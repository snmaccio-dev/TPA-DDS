package donatrack.logistica.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.EstadoRuta;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositorioRutasTest implements SimplePersistenceTest {

  private final RepositorioRutas repositorio = RepositorioRutas.getInstance();

  private Entrega entregaMedida(long donacionId, String direccion) {
    Entrega entrega = new Entrega(donacionId, 900L, direccion, "Alimentos", 5, "cajas");
    entrega.registrarMedicion(4.0, 0.2);
    persist(entrega);
    return entrega;
  }

  @Test
  void unaRutaGuardadaArrastraSusDestinosYSeRecuperaCompleta() {
    Camion camion = new Camion("RUT111", 40.0, 3.0, 2000.0);
    persist(camion);

    RutaReparto ruta = new RutaReparto(camion, List.of(
        new DestinoEntrega(0, "Primera 100", List.of(entregaMedida(401L, "Primera 100"))),
        new DestinoEntrega(1, "Segunda 200", List.of(entregaMedida(402L, "Segunda 200")))
    ));
    repositorio.guardar(ruta);

    entityManager().flush();
    entityManager().clear();

    RutaReparto recuperada = repositorio.buscarPorId(ruta.getId()).orElseThrow();

    assertNotNull(recuperada.getId());
    assertEquals("RUT111", recuperada.getCamion().getPatente());
    assertEquals(EstadoRuta.PLANIFICADA, recuperada.getEstado());
    assertEquals(2, recuperada.getDestinos().size());
    assertEquals(2, recuperada.getEntregas().size());
  }

  @Test
  void elOrdenDelRecorridoSobreviveAlIdaYVuelta() {
    Camion camion = new Camion("RUT222", 40.0, 3.0, 2000.0);
    persist(camion);

    RutaReparto ruta = new RutaReparto(camion, List.of(
        new DestinoEntrega(0, "Primera parada", List.of(entregaMedida(501L, "Primera parada"))),
        new DestinoEntrega(1, "Segunda parada", List.of(entregaMedida(502L, "Segunda parada"))),
        new DestinoEntrega(2, "Tercera parada", List.of(entregaMedida(503L, "Tercera parada")))
    ));
    repositorio.guardar(ruta);

    entityManager().flush();
    entityManager().clear();

    List<String> direcciones = repositorio.buscarPorId(ruta.getId()).orElseThrow()
        .getDestinos().stream()
        .map(DestinoEntrega::getDireccion)
        .toList();

    assertEquals(
        List.of("Primera parada", "Segunda parada", "Tercera parada"),
        direcciones
    );
  }
}
