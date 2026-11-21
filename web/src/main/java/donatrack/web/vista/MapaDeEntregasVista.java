package donatrack.web.vista;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mapa publico de entregas: un marcador por entidad beneficiaria con coordenadas, con
// todas sus entregas confirmadas (asi una entidad que recibio varias no queda con un
// marcador encima de otro). Las entregas de entidades sin coordenadas no se dibujan; se
// cuentan para avisarlo en la pantalla.
public record MapaDeEntregasVista(List<Marcador> marcadores, int entregasSinUbicacion) {

  public record Marcador(long entidadId,
                         String razonSocial,
                         double latitud,
                         double longitud,
                         List<EntregaEnMapa> entregas) {
  }

  public record EntregaEnMapa(String descripcion, String cantidad, String fecha, String foto) {
  }

  public MapaDeEntregasVista {
    marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
  }

  public static MapaDeEntregasVista vacio(List<EntregaPublicaVista> entregas) {
    return new MapaDeEntregasVista(List.of(), entregas == null ? 0 : entregas.size());
  }

  public static MapaDeEntregasVista de(List<EntregaPublicaVista> entregas,
                                       List<UbicacionEntidadVista> ubicaciones) {
    Map<Long, List<EntregaEnMapa>> porEntidad = new LinkedHashMap<>();
    for (EntregaPublicaVista entrega : entregas) {
      porEntidad.computeIfAbsent(entrega.beneficiariaId(), id -> new ArrayList<>())
          .add(new EntregaEnMapa(
              entrega.getDescripcion(),
              entrega.getCantidadTexto(),
              entrega.getFechaHoraEntrega(),
              entrega.getPrimeraFoto()));
    }

    List<Marcador> marcadores = new ArrayList<>();
    int ubicadas = 0;
    for (UbicacionEntidadVista ubicacion : ubicaciones) {
      List<EntregaEnMapa> suyas = porEntidad.get(ubicacion.id());
      if (suyas == null) {
        continue;
      }
      ubicadas += suyas.size();
      marcadores.add(new Marcador(ubicacion.id(), ubicacion.razonSocial(),
          ubicacion.latitud(), ubicacion.longitud(), suyas));
      porEntidad.remove(ubicacion.id());
    }
    return new MapaDeEntregasVista(marcadores, entregas.size() - ubicadas);
  }

  public List<Marcador> getMarcadores() {
    return marcadores;
  }

  public boolean isTieneMarcadores() {
    return !marcadores.isEmpty();
  }

  public int getEntregasSinUbicacion() {
    return entregasSinUbicacion;
  }

  public boolean isHayEntregasSinUbicacion() {
    return entregasSinUbicacion > 0;
  }

  // Para el <script type="application/json"> que lee app.js.
  public String getJson() {
    return MapaJson.de(marcadores);
  }
}
