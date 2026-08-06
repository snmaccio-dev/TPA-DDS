package donatrack.model.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;

public class PrioridadSubatendidos implements Algoritmo {

  @Override
  public List<EntidadBeneficiaria> matchmaking(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    return entidades.stream()
        .sorted(Comparator.comparingLong(
            EntidadBeneficiaria::getCantidadDonacionesUltimoTrimestre))
        .limit(10)
        .toList();
  }
}