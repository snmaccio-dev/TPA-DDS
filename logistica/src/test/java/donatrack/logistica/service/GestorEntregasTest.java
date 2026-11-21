package donatrack.logistica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.EventoRutasPlanificadas;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class GestorEntregasTest implements SimplePersistenceTest {

  private static final long BENEFICIARIA = 9301L;
  private static final long OTRA_BENEFICIARIA = 9302L;

  private final GestorEntregas gestor = new GestorEntregas(
      new DonacionesQueNoHaceNada(), "http://monitoreo.test", foto -> foto.nombre());

  @Test
  void soloConfirmablesDejaLasEntregasEnTrasladoDeLaBeneficiaria() {
    Entrega enTraslado = entregaEnTraslado(901L, BENEFICIARIA, "CNF111");
    Entrega pendiente = new Entrega(902L, 500L, BENEFICIARIA, "Destino", "Arroz", 5, "kg");
    Entrega deOtra = entregaEnTraslado(903L, OTRA_BENEFICIARIA, "CNF222");
    persist(pendiente);
    entityManager().flush();

    List<Long> confirmables = idsDeDonacion(gestor.deBeneficiaria(BENEFICIARIA, true));
    List<Long> todas = idsDeDonacion(gestor.deBeneficiaria(BENEFICIARIA, false));

    assertEquals(List.of(enTraslado.getDonacionId()), confirmables);
    assertEquals(2, todas.size());
    assertTrue(todas.containsAll(List.of(901L, 902L)));
    assertFalse(todas.contains(deOtra.getDonacionId()));
  }

  @Test
  void unaEntregaConfirmadaYaNoApareceComoConfirmable() {
    Entrega entrega = entregaEnTraslado(911L, BENEFICIARIA, "CNF333");
    entityManager().flush();

    entrega.confirmarRecepcion(List.of("foto.jpg"));
    entityManager().flush();

    assertTrue(gestor.deBeneficiaria(BENEFICIARIA, true).isEmpty());
    assertEquals(1, gestor.deBeneficiaria(BENEFICIARIA, false).size());
  }

  private Entrega entregaEnTraslado(long donacionId, long beneficiariaId, String patente) {
    Camion camion = new Camion(patente, 40.0, 3.0, 2000.0);
    Chofer chofer = new Chofer("Ana", "Ramirez", "30" + donacionId, "B1");
    persist(camion);
    persist(chofer);

    Entrega entrega = new Entrega(
        donacionId, 500L, beneficiariaId, "Destino", "Frazadas", 20, "unidades");
    entrega.registrarMedicion(12.0, 0.5);
    persist(entrega);

    RutaReparto ruta = new RutaReparto(
        camion,
        List.of(new DestinoEntrega(0, "Destino", List.of(entrega)))
    );
    persist(ruta);
    ruta.iniciarRuta(chofer);

    return entrega;
  }

  private static List<Long> idsDeDonacion(List<Entrega> entregas) {
    return entregas.stream().map(Entrega::getDonacionId).toList();
  }

  private static class DonacionesQueNoHaceNada implements ClienteDonaciones {

    @Override
    public List<DonacionParaPlanificar> donacionesConAsignacionRealizada() {
      return List.of();
    }

    @Override
    public void publicarRutasPlanificadas(EventoRutasPlanificadas evento) {
    }

    @Override
    public void publicarInicioRuta(EventoInicioRuta evento) {
    }

    @Override
    public void publicarEntrega(EventoEntrega evento) {
    }

    @Override
    public void publicarRetornoADeposito(EventoRetornoDeposito evento) {
    }
  }
}
