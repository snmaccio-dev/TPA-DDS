package donatrack.logistica.infrastructure.planificacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.contrato.DestinoPlanificado;
import donatrack.logistica.contrato.PlanificacionRecibida;
import donatrack.logistica.contrato.RutaPlanificada;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.infrastructure.integracion.SerializadorJackson;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GeneradorRutasSimuladoTest {

  private static final String TOKEN = "token-de-prueba";
  private static final String CALLBACK = "http://logistica.test/planificacion/callback";

  private final ScheduledExecutorService ejecutor = Executors.newSingleThreadScheduledExecutor();

  @AfterEach
  void apagar() {
    ejecutor.shutdownNow();
  }

  @Test
  void cargaCadaEntregaEnElPrimerCamionDondeEntraYAgrupaPorDireccion() {
    Camion chico = new Camion("CHICO1", 1.0, 2.0, 100.0);
    Camion grande = new Camion("GRANDE1", 10.0, 2.5, 1000.0);
    Entrega primera = entrega(1, "Brandsen 805", 60, 0.5);
    Entrega noEntraEnElChico = entrega(2, "Av. Rivadavia 1864", 50, 0.4);
    Entrega mismaDireccion = entrega(3, "Brandsen 805", 30, 0.3);
    Entrega demasiadoPesada = entrega(4, "Av. Sarmiento 2601", 2000, 0.1);

    PlanificacionRecibida plan = GeneradorRutasSimulado.armarPlan("sol-1",
        List.of(primera, noEntraEnElChico, mismaDireccion, demasiadoPesada),
        List.of(chico, grande));

    assertEquals("sol-1", plan.solicitudId());
    assertEquals(List.of(
        new RutaPlanificada("CHICO1",
            List.of(new DestinoPlanificado("Brandsen 805", List.of(1L, 3L)))),
        new RutaPlanificada("GRANDE1",
            List.of(new DestinoPlanificado("Av. Rivadavia 1864", List.of(2L))))
    ), plan.rutas());
    assertEquals(List.of(4L), plan.entregaIdsNoAsignadas());
  }

  @Test
  void respetaElVolumenAdemasDelPeso() {
    Camion camion = new Camion("VOL1", 1.0, 2.0, 1000.0);

    PlanificacionRecibida plan = GeneradorRutasSimulado.armarPlan("sol-2",
        List.of(entrega(1, "Calle 1", 10, 0.7), entrega(2, "Calle 2", 10, 0.7)),
        List.of(camion));

    assertEquals(1, plan.rutas().size());
    assertEquals(List.of(2L), plan.entregaIdsNoAsignadas());
  }

  @Test
  void sinCamionesTodasQuedanNoAsignadas() {
    PlanificacionRecibida plan = GeneradorRutasSimulado.armarPlan("sol-3",
        List.of(entrega(1, "Calle 1", 10, 0.1), entrega(2, "Calle 2", 10, 0.1)), List.of());

    assertTrue(plan.rutas().isEmpty());
    assertEquals(List.of(1L, 2L), plan.entregaIdsNoAsignadas());
  }

  @Test
  void respondeElCallbackEnOtroHiloConElTokenYSinUsarCamionesOcupados() throws Exception {
    CountDownLatch respondio = new CountDownLatch(1);
    AtomicReference<String> url = new AtomicReference<>();
    AtomicReference<String> token = new AtomicReference<>();
    AtomicReference<String> cuerpo = new AtomicReference<>();
    AtomicReference<String> hilo = new AtomicReference<>();
    GeneradorRutasSimulado simulado = new GeneradorRutasSimulado(
        TOKEN, new SerializadorJackson(), () -> Set.of("OCUPADO1"), Duration.ZERO, ejecutor,
        (destino, tokenEnviado, json) -> {
          url.set(destino);
          token.set(tokenEnviado);
          cuerpo.set(json);
          hilo.set(Thread.currentThread().getName());
          respondio.countDown();
          return 200;
        });

    simulado.solicitarPlanificacion("sol-4",
        List.of(entrega(7, "Brandsen 805", 20, 0.3)),
        List.of(new Camion("OCUPADO1", 18, 2.6, 4000), new Camion("LIBRE1", 18, 2.6, 4000)),
        CALLBACK);

    assertTrue(respondio.await(5, TimeUnit.SECONDS), "El simulado no llamo al callback.");
    assertEquals(CALLBACK, url.get());
    assertEquals(TOKEN, token.get());
    assertTrue(!Thread.currentThread().getName().equals(hilo.get()),
        "El callback tiene que salir de otro hilo.");

    PlanificacionRecibida enviado =
        new SerializadorJackson().desdeJson(cuerpo.get(), PlanificacionRecibida.class);
    assertEquals("sol-4", enviado.solicitudId());
    assertEquals(List.of(new RutaPlanificada("LIBRE1",
            List.of(new DestinoPlanificado("Brandsen 805", List.of(7L))))),
        enviado.rutas());
    assertTrue(enviado.entregaIdsNoAsignadas().isEmpty());
  }

  @Test
  void unCallbackQueFallaNoRompeLaSolicitud() throws Exception {
    CountDownLatch intento = new CountDownLatch(1);
    GeneradorRutasSimulado simulado = new GeneradorRutasSimulado(
        TOKEN, new SerializadorJackson(), Set::of, Duration.ZERO, ejecutor,
        (destino, tokenEnviado, json) -> {
          intento.countDown();
          throw new IllegalStateException("Logistica no responde");
        });

    assertDoesNotThrow(() -> simulado.solicitarPlanificacion("sol-5",
        List.of(entrega(8, "Calle 1", 10, 0.1)),
        List.of(new Camion("LIBRE1", 18, 2.6, 4000)), CALLBACK));
    assertTrue(intento.await(5, TimeUnit.SECONDS));
  }

  private static Entrega entrega(long id, String direccion, double pesoKg, double volumenM3) {
    Entrega entrega = new Entrega(100 + id, 500L, 900L, direccion, "Donacion " + id, 1, "unidad");
    entrega.registrarMedicion(pesoKg, volumenM3);
    try {
      Field campo = Entrega.class.getDeclaredField("id");
      campo.setAccessible(true);
      campo.set(entrega, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    return entrega;
  }
}
