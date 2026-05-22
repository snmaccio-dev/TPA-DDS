package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DonacionBuilderTest {

  @Test
  public void builderCreaDonacionExitosamenteConDatosCompletos() {
    Subcategoria categoria = subcategoriaSillas();
    Donacion donacion = new DonacionBuilder()
        .conSubcategoria(categoria)
        .agregarBienes(List.of(silla(categoria)))
        .build();

    assertNotNull(donacion);
    assertEquals(categoria, donacion.getSubcategoria());
    assertEquals(1, donacion.getBienes().size());
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());
  }

  @Test
  public void builderLanzaExcepcionSiFaltaSubcategoria() {
    DonacionBuilder builder = new DonacionBuilder()
        .agregarBienes(List.of(silla(subcategoriaSillas())));

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void builderLanzaExcepcionSiListaDeBienesEstaVacia() {
    DonacionBuilder builder = new DonacionBuilder()
        .conSubcategoria(subcategoriaSillas());

    assertThrows(IllegalStateException.class, builder::build);
  }

  private Subcategoria subcategoriaSillas() {
    return new Subcategoria("Sillas");
  }

  private Bien silla(Subcategoria subcategoria) {
    return new Bien("Silla de oficina", subcategoria, Unidades.UNIDADES);
  }
}