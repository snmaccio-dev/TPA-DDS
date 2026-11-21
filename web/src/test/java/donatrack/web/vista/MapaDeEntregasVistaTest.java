package donatrack.web.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapaDeEntregasVistaTest {

  @Test
  void unaEntidadConVariasEntregasTieneUnSoloMarcadorConTodas() {
    List<EntregaPublicaVista> entregas = List.of(
        entrega(1, 10, "Leche", "2026-11-20T10:00:00", "https://fotos/1.png"),
        entrega(2, 20, "Arroz", "2026-11-19T10:00:00", null),
        entrega(3, 10, "Aceite", "2026-11-18T10:00:00", null));

    MapaDeEntregasVista mapa = MapaDeEntregasVista.de(entregas, List.of(
        new UbicacionEntidadVista(10, "Comedor", -34.63, -58.36),
        new UbicacionEntidadVista(20, "Escuela", -34.60, -58.39)));

    assertEquals(2, mapa.getMarcadores().size());
    MapaDeEntregasVista.Marcador comedor = mapa.getMarcadores().get(0);
    assertEquals("Comedor", comedor.razonSocial());
    assertEquals(List.of("Leche", "Aceite"),
        comedor.entregas().stream().map(MapaDeEntregasVista.EntregaEnMapa::descripcion).toList());
    assertEquals("https://fotos/1.png", comedor.entregas().get(0).foto());
    assertFalse(mapa.isHayEntregasSinUbicacion());
  }

  @Test
  void lasEntregasDeEntidadesSinCoordenadasNoSeDibujanPeroSeCuentan() {
    List<EntregaPublicaVista> entregas = List.of(
        entrega(1, 10, "Leche", "2026-11-20T10:00:00", null),
        entrega(2, 30, "Ropa", "2026-11-19T10:00:00", null),
        entrega(3, 30, "Mesas", "2026-11-18T10:00:00", null));

    MapaDeEntregasVista mapa = MapaDeEntregasVista.de(entregas, List.of(
        new UbicacionEntidadVista(10, "Comedor", -34.63, -58.36)));

    assertEquals(1, mapa.getMarcadores().size());
    assertTrue(mapa.isHayEntregasSinUbicacion());
    assertEquals(2, mapa.getEntregasSinUbicacion());
  }

  @Test
  void elJsonNoPuedeCerrarElScriptQueLoContiene() {
    MapaDeEntregasVista mapa = MapaDeEntregasVista.de(
        List.of(entrega(1, 10, "</script><b>x", "2026-11-20T10:00:00", null)),
        List.of(new UbicacionEntidadVista(10, "Comedor", -34.63, -58.36)));

    assertFalse(mapa.getJson().contains("<"));
    assertTrue(mapa.getJson().contains("\u003c/script>"));
  }

  @Test
  void recientesSonLasCincoUltimasPorFechaDeEntrega() {
    List<EntregaPublicaVista> entregas = List.of(
        entrega(1, 10, "A", "2026-11-14T10:00:00", null),
        entrega(2, 10, "B", "2026-11-20T10:00:00", null),
        entrega(3, 10, "C", "2026-11-16T10:00:00", null),
        entrega(4, 10, "D", "2026-11-19T10:00:00", null),
        entrega(5, 10, "E", "2026-11-15T10:00:00", null),
        entrega(6, 10, "F", "2026-11-18T10:00:00", null),
        entrega(7, 10, "G", "2026-11-17T10:00:00", null));

    assertEquals(List.of(2L, 4L, 6L, 7L, 3L),
        EntregaPublicaVista.recientes(entregas, 5).stream()
            .map(EntregaPublicaVista::entregaId).toList());
  }

  private static EntregaPublicaVista entrega(long id, long beneficiariaId, String descripcion,
                                             String fecha, String foto) {
    return new EntregaPublicaVista(id, 100 + id, beneficiariaId, descripcion, 2, "kg", fecha,
        foto == null ? List.of() : List.of(foto));
  }
}
