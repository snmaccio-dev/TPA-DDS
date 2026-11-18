package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Donante;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SegmentadorDonaciones {

  public List<Donacion> segmentar(List<Bien> bienes,
                                  Donante donante,
                                  String descripcion) {
    return segmentar(
        bienes,
        donante,
        descripcion,
        EstadosPosiblesDonacion.EN_DEPOSITO
    );
  }

  public List<Donacion> segmentar(List<Bien> bienes,
                                  Donante donante,
                                  String descripcion,
                                  EstadosPosiblesDonacion estadoInicial) {

    Map<Clave, List<Bien>> grupos = new LinkedHashMap<>();

    for (Bien bien : bienes) {
      Clave clave = new Clave(
          bien.getSubcategoria(),
          bien.getCondicion(),
          bien.getFechaVencimiento()
      );

      grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(bien);
    }

    return grupos.values().stream()
        .map(grupo -> Donacion.crear(grupo, donante, descripcion, estadoInicial))
        .toList();
  }

  private record Clave(Subcategoria subcategoria,
                       CondicionBien condicion,
                       LocalDate fechaVencimiento) {}
}
