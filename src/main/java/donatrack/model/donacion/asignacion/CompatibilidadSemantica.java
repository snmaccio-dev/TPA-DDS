package donatrack.model.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.necesidad.Necesidad;
import donatrack.model.persona.Beneficiaria;

public class CompatibilidadSemantica implements Algoritmo {

  @Override
  public List<Beneficiaria> matchmaking(
      Donacion donacion,
      List<Beneficiaria> beneficiarias) {

    return beneficiarias.stream()
        .sorted(Comparator.comparingInt(
            beneficiaria -> -puntajeCompatibilidad(beneficiaria, donacion)))
        .limit(10)
        .toList();
  }

  private int puntajeCompatibilidad(Beneficiaria beneficiaria,
                                    Donacion donacion) {

    int puntaje = 0;

    for (Necesidad necesidad : beneficiaria.getNecesidades()) {

      if (necesidad.getSubcategoria().equals(donacion.getSubcategoria())) {
        puntaje += necesidad.getCantidad();
      }
    }

    return puntaje;
  }
}
