package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DonacionBuilderTest {

  private Subcategoria subcategoriaSillas;
  private Bien silla;

  @BeforeEach
  void setUp() {
    subcategoriaSillas = new Subcategoria("Sillas");
    silla = new Bien("Silla de oficina", subcategoriaSillas, Unidades.UNIDADES);
  }

  @Test
  void builderCreaDonacionExitosamenteConDatosCompletos() {
    Donacion donacion = new DonacionBuilder()
        .conSubcategoria(subcategoriaSillas)
        .agregarBienes(List.of(silla))
        .build();

    assertNotNull(donacion);
    assertEquals(subcategoriaSillas, donacion.getSubcategoria());
    assertEquals(1, donacion.getBienes().size());
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());
  }

  @Test
  void builderLanzaExcepcionSiFaltaSubcategoria() {
    DonacionBuilder builder = new DonacionBuilder()
        .agregarBienes(List.of(silla));

    assertThrows(IllegalStateException.class, builder::build,
        "El builder debería fallar si no se le asigna una subcategoría");
  }

  @Test
  void builderLanzaExcepcionSiListaDeBienesEstaVacia() {
    DonacionBuilder builder = new DonacionBuilder()
        .conSubcategoria(subcategoriaSillas);

    assertThrows(IllegalStateException.class, builder::build,
        "El builder debería fallar si la lista de bienes está vacía o es nula");
  }
}
