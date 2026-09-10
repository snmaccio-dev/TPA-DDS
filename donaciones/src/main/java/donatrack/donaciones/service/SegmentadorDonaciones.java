package donatrack.donaciones.service;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
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
        .map(grupo -> Donacion.crear(grupo, donante, descripcion))
        .toList();
  }

  private record Clave(Subcategoria subcategoria,
                       CondicionBien condicion,
                       LocalDate fechaVencimiento) {}
}
