package donatrack.donaciones.domain.persona;

import donatrack.donaciones.domain.DatosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordenadasTest {

  @Test
  void sinNingunaDeLasDosNoHayUbicacion() {
    assertNull(Coordenadas.opcionales(null, null));
  }

  @Test
  void conLasDosQuedaLaUbicacion() {
    assertEquals(new Coordenadas(-34.6356, -58.3647), Coordenadas.opcionales(-34.6356, -58.3647));
  }

  @Test
  void conUnaSolaEsUnError() {
    assertThrows(DatosInvalidosException.class, () -> Coordenadas.opcionales(-34.6, null));
    assertThrows(DatosInvalidosException.class, () -> Coordenadas.opcionales(null, -58.4));
  }

  @Test
  void fueraDeRangoEsUnError() {
    assertThrows(DatosInvalidosException.class, () -> new Coordenadas(-91, 0));
    assertThrows(DatosInvalidosException.class, () -> new Coordenadas(0, 181));
    assertThrows(DatosInvalidosException.class, () -> new Coordenadas(Double.NaN, 0));
  }
}
