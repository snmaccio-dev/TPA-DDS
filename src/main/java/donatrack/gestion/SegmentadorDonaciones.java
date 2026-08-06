package donatrack.gestion;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegmentadorDonaciones {

  public List<Donacion> segmentar(List<Bien> bienes, Persona donante) {

    Map<Subcategoria, List<Bien>> grupos = bienes.stream()
        .collect(Collectors.groupingBy(Bien::getSubcategoria));

    return grupos.entrySet()
        .stream()
        .map(entry -> Donacion.crear(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
