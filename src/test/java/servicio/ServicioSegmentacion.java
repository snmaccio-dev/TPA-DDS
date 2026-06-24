package servicio;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Persona;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicioSegmentacion {

  public List<Donacion> segmentar(List<Bien> bienes, Persona donante) {
    Map<String, List<Bien>> grupos = agruparPorSubcategoria(bienes);
    List<Donacion> resultado = new ArrayList<>();

    for (Map.Entry<String, List<Bien>> entry : grupos.entrySet()) {
      Subcategoria subcategoria = entry.getValue().get(0).getSubcategoria();

      Donacion donacion = new DonacionBuilder()
          .conSubcategoria(subcategoria)
          .agregarBienes(entry.getValue())
          .build();

      resultado.add(donacion);
    }

    return resultado;
  }

    private Map<String, List<Bien>> agruparPorSubcategoria(List<Bien> bienes) {
        Map<String, List<Bien>> grupos = new LinkedHashMap<>();
        for (Bien bien : bienes) {
            String clave = bien.getSubcategoria().getNombre();
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(bien);
        }
        return grupos;
    }
}
