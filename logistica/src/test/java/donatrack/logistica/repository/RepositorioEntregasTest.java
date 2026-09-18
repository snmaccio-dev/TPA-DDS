package donatrack.logistica.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepositorioEntregasTest implements SimplePersistenceTest {

  private final RepositorioEntregas repositorio = RepositorioEntregas.getInstance();

  private Entrega nuevaEntrega(long donacionId) {
    return new Entrega(donacionId, 900L, "Av. Siempreviva 742", "Frazadas", 20, "unidades");
  }

  @Test
  void unaEntregaGuardadaSeRecuperaConTodosSusDatos() {
    repositorio.guardar(nuevaEntrega(101L));

    entityManager().flush();
    entityManager().clear();

    Optional<Entrega> recuperada = repositorio.buscarPorDonacion(101L);

    assertTrue(recuperada.isPresent());
    assertNotNull(recuperada.get().getId());
    assertEquals(900L, recuperada.get().getBeneficiariaId());
    assertEquals("Av. Siempreviva 742", recuperada.get().getDireccionDestino());
    assertEquals("Frazadas", recuperada.get().getDescripcionDonacion());
    assertEquals(20, recuperada.get().getCantidad());
    assertEquals("unidades", recuperada.get().getUnidad());
  }

  @Test
  void lasFotosSobrevivenAlIdaYVueltaContraLaBase() {
    Camion camion = new Camion("FOT111", 30.0, 2.5, 1000.0);
    Chofer chofer = new Chofer("Ana", "Ramirez", "30999888", "B1");
    persist(camion);
    persist(chofer);

    Entrega entrega = nuevaEntrega(102L);
    entrega.registrarMedicion(12.0, 0.5);
    repositorio.guardar(entrega);

    RutaReparto ruta = new RutaReparto(
        camion,
        List.of(new DestinoEntrega(0, "Av. Siempreviva 742", List.of(entrega)))
    );
    persist(ruta);

    ruta.iniciarRuta(chofer);
    entrega.confirmarRecepcion(List.of("foto-1.jpg", "foto-2.jpg"));

    entityManager().flush();
    entityManager().clear();

    Entrega recuperada = repositorio.buscarPorDonacion(102L).orElseThrow();

    assertEquals(List.of("foto-1.jpg", "foto-2.jpg"), recuperada.getFotos());
  }

  @Test
  void queEsperanPlanificacionSoloTraeLasPendientesYaMedidas() {
    Entrega medida = nuevaEntrega(201L);
    medida.registrarMedicion(10.0, 1.0);
    repositorio.guardar(medida);

    repositorio.guardar(nuevaEntrega(202L));

    entityManager().flush();

    List<Long> donaciones = repositorio.queEsperanPlanificacion().stream()
        .map(Entrega::getDonacionId)
        .toList();

    assertTrue(donaciones.contains(201L));
    assertFalse(donaciones.contains(202L));
  }

  @Test
  void pendientesDeMedicionSoloTraeLasQueNoTienenPesoNiVolumen() {
    repositorio.guardar(nuevaEntrega(301L));

    Entrega medida = nuevaEntrega(302L);
    medida.registrarMedicion(8.0, 0.4);
    repositorio.guardar(medida);

    entityManager().flush();

    List<Long> donaciones = repositorio.pendientesDeMedicion().stream()
        .map(Entrega::getDonacionId)
        .toList();

    assertTrue(donaciones.contains(301L));
    assertFalse(donaciones.contains(302L));
  }

  @Test
  void buscarPorDonacionNoDevuelveNadaSiLaDonacionNoFueIncorporada() {
    assertTrue(repositorio.buscarPorDonacion(999999L).isEmpty());
  }
}
