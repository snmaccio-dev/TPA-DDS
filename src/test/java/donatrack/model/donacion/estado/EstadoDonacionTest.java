package donatrack.model.donacion.estado;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EstadoDonacionTest {

  private Donacion donacion;

  @BeforeEach
  void setUp() {
    Subcategoria ropa = new Subcategoria("Camperas de abrigo");
    Bien campera = new Bien("Campera talle M nueva", ropa, Unidades.UNIDADES);
    donacion = new Donacion(List.of(campera), ropa);
  }

  @Test
  void donacionHaceElRecorridoFelizHastaSerEntregada() {
    donacion.asignar();
    assertEquals("ASIGNACION_REALIZADA", donacion.getEstado().getNombre());

    donacion.planificarRuta();
    assertEquals("LISTA_PARA_ENTREGAR", donacion.getEstado().getNombre());

    donacion.iniciarTraslado();
    assertEquals("EN_TRASLADO", donacion.getEstado().getNombre());

    donacion.confirmarEntrega();
    assertEquals("ENTREGADA", donacion.getEstado().getNombre());
  }

  @Test
  void donacionEnTrasladoFallaYRetornaAlDeposito() {
    donacion.asignar();
    donacion.planificarRuta();
    donacion.iniciarTraslado();

    donacion.fallarEntrega("No había nadie en la escuela");
    assertTrue(donacion.getEstado() instanceof EntregaFallida);

    ((EntregaFallida) donacion.getEstado()).devolverAlDeposito(donacion);
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());
  }

  @Test
  void transicionInvalidaLanzaIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> {
      donacion.confirmarEntrega();
    }, "Debería lanzar IllegalStateException al intentar confirmar entrega desde el depósito");
  }
}