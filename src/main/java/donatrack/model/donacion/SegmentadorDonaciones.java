package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;

import java.util.*;
import java.util.stream.Collectors;

public class SegmentadorDonaciones {

  public static List<Donacion> segmentar(List<Bien> bienes) {

    Map<Subcategoria, List<Bien>> grupos =
        bienes.stream()
            .collect(Collectors.groupingBy(Bien::getSubcategoria));

    return grupos.entrySet()
        .stream()
        .map(entry ->
            new Donacion(entry.getValue(), entry.getKey())
        )
        .collect(Collectors.toList());
  }
}
