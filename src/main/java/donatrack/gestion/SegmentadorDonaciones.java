package donatrack.gestion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.CondicionBien;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SegmentadorDonaciones {

  public List<Donacion> segmentar(List<Bien> bienes,
                                  Persona donante,
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
