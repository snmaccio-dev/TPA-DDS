package donatrack.model.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.model.necesidad.Necesidad;

public class CompatibilidadSemantica implements Algoritmo {

  @Override
  public List<EntidadBeneficiaria> matchmaking(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    return entidades.stream()
        .sorted(Comparator.comparingInt(
            entidad -> -puntajeCompatibilidad(entidad, donacion)))
        .limit(10)
        .toList();
  }

  private int puntajeCompatibilidad(EntidadBeneficiaria entidad,
                                    Donacion donacion) {

    int puntaje = 0;

    for (Necesidad necesidad : entidad.getNecesidades()) {

      if (necesidad.getSubcategoria().equals(donacion.getSubcategoria())) {
        puntaje += necesidad.getCantidad();
      }
    }

    return puntaje;
  }
}
