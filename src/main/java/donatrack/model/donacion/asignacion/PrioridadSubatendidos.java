package donatrack.model.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

public class PrioridadSubatendidos implements Algoritmo {

  @Override
  public List<Beneficiaria> matchmaking(
      Donacion donacion,
      List<Beneficiaria> beneficiarias) {

    return beneficiarias.stream()
        .sorted(Comparator.comparingLong(
            Beneficiaria::getCantidadDonacionesUltimoTrimestre))
        .limit(10)
        .toList();
  }
}
