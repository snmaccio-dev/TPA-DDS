package donatrack.model.donacion.estado;

import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.CondicionBien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.Genero;
import donatrack.model.persona.PersonaHumana;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class EstadoDonacionTest {

  @Test
  public void donacionHaceElRecorridoFelizHastaSerEntregada() {
    Donacion donacion = donacionDeCampera();

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
  public void donacionEnTrasladoFallaYRetornaAlDeposito() {
    Donacion donacion = donacionDeCampera();
    donacion.asignar();
    donacion.planificarRuta();
    donacion.iniciarTraslado();

    donacion.fallarEntrega("No había nadie en la escuela");
    assertTrue(donacion.getEstado() instanceof EntregaFallida);

    ((EntregaFallida) donacion.getEstado()).devolverAlDeposito(donacion);
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());
  }

  @Test
  public void transicionInvalidaLanzaIllegalStateException() {
    Donacion donacion = donacionDeCampera();

    assertThrows(IllegalStateException.class, () -> {
      donacion.confirmarEntrega();
    });
  }

  private Donacion donacionDeCampera() {
    Subcategoria ropa = new Subcategoria("Camperas de abrigo", new Categoria("Vestimenta"));
    Bien campera = new Bien("Campera talle M nueva", ropa, 1, Unidades.UNIDADES, CondicionBien.NUEVO);
    PersonaHumana donante = new PersonaHumana("Test", "Donante", 30, "0", Genero.MASCULINO);
    return new Donacion(List.of(campera), ropa, donante, "Campera nueva");
  }
}