package donatrack.logistica.domain.entrega;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import donatrack.logistica.domain.flota.Camion;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.ruta.DestinoEntrega;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntregaTest implements SimplePersistenceTest {

  @Test
  void retornarADepositoDesvinculaLaEntregaDeSuRutaYDeSuDestino() {
    Camion camion = new Camion("RET111", 40.0, 3.0, 2000.0);
    Chofer chofer = new Chofer("Luis", "Gomez", "26555666", "C2");
    persist(camion);
    persist(chofer);

    Entrega entrega = new Entrega(701L, 900L, "Calle Falsa 123", "Ropa", 3, "bolsas");
    entrega.registrarMedicion(6.0, 0.3);
    persist(entrega);

    RutaReparto ruta = new RutaReparto(
        camion,
        List.of(new DestinoEntrega(0, "Calle Falsa 123", List.of(entrega)))
    );
    persist(ruta);

    ruta.iniciarRuta(chofer);
    entrega.marcarNoRecibida("Nadie atendio");
    entrega.retornarADeposito();

    entityManager().flush();
    entityManager().clear();

    Entrega recuperada = createQuery(
        "select e from Entrega e where e.donacionId = :donacionId",
        Entrega.class)
        .setParameter("donacionId", 701L)
        .getSingleResult();

    assertEquals(EstadoEntrega.PENDIENTE, recuperada.getEstado());
    assertNull(recuperada.getRuta());
    assertNull(recuperada.getPatenteCamion());
    assertEquals(List.of(), recuperada.getFotos());

    Long sinDestino = createQuery(
        "select count(e) from Entrega e "
            + "where e.donacionId = :donacionId and e.destino is null",
        Long.class)
        .setParameter("donacionId", 701L)
        .getSingleResult();

    assertEquals(1L, sinDestino);
  }
}
