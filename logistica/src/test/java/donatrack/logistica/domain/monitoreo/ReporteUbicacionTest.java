package donatrack.logistica.domain.monitoreo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReporteUbicacionTest implements SimplePersistenceTest {

  private RutaReparto rutaEnCurso(String patente, String documento, long donacionId) {
    Camion camion = new Camion(patente, 40.0, 3.0, 2000.0);
    Chofer chofer = new Chofer("Ana", "Ramirez", documento, "B1");
    persist(camion);
    persist(chofer);

    Entrega entrega = new Entrega(donacionId, 900L, "Destino 1", "Alimentos", 5, "cajas");
    entrega.registrarMedicion(4.0, 0.2);
    persist(entrega);

    RutaReparto ruta = new RutaReparto(
        camion,
        List.of(new DestinoEntrega(0, "Destino 1", List.of(entrega)))
    );
    persist(ruta);
    ruta.iniciarRuta(chofer);

    return ruta;
  }

  @Test
  void elHistoricoGuardaTodosLosReportesDeLaRuta() {
    RutaReparto ruta = rutaEnCurso("MON111", "28111222", 601L);
    LocalDateTime base = LocalDateTime.of(2026, 9, 17, 10, 0);

    persist(new ReporteUbicacion(ruta, "MON111", -34.60, -58.38, 40.0, base));
    persist(new ReporteUbicacion(ruta, "MON111", -34.61, -58.39, 45.0, base.plusMinutes(5)));
    persist(new ReporteUbicacion(ruta, "MON111", -34.62, -58.40, 50.0, base.plusMinutes(10)));

    entityManager().flush();

    Long cantidad = createQuery(
        "select count(r) from ReporteUbicacion r where r.ruta = :ruta",
        Long.class)
        .setParameter("ruta", ruta)
        .getSingleResult();

    assertEquals(3L, cantidad);
  }

  @Test
  void laRutaConservaElUltimoReporteAunqueLleguenDesordenados() {
    RutaReparto ruta = rutaEnCurso("MON222", "28333444", 602L);
    LocalDateTime base = LocalDateTime.of(2026, 9, 17, 10, 0);

    ReporteUbicacion masNuevo =
        new ReporteUbicacion(ruta, "MON222", -34.62, -58.40, 50.0, base.plusMinutes(10));
    ReporteUbicacion atrasado =
        new ReporteUbicacion(ruta, "MON222", -34.60, -58.38, 40.0, base);

    persist(masNuevo);
    persist(atrasado);
    ruta.registrarUbicacion(masNuevo);
    ruta.registrarUbicacion(atrasado);

    entityManager().flush();
    entityManager().clear();

    RutaReparto recuperada = find(RutaReparto.class, ruta.getId());

    assertNotNull(recuperada.getUltimoReporte());
    assertEquals(base.plusMinutes(10), recuperada.getUltimoReporte().getMomento());
    assertEquals(50.0, recuperada.getUltimoReporte().getVelocidadKmh());
  }
}
