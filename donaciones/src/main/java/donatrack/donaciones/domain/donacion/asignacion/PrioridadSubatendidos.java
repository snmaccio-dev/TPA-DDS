package donatrack.donaciones.domain.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

// Prioriza a las entidades que menos donaciones recibieron en el ultimo trimestre. A
// igual cantidad, por id, asi el ranking es el mismo en cada corrida.
public class PrioridadSubatendidos implements Algoritmo {

  @Override
  public List<Beneficiaria> matchmaking(
      Donacion donacion,
      List<Beneficiaria> candidatas) {

    return candidatas.stream()
        .sorted(Comparator.comparingLong(Beneficiaria::getCantidadDonacionesUltimoTrimestre)
            .thenComparing(Desempate.POR_ID))
        .limit(MAXIMO_DE_CANDIDATAS)
        .toList();
  }
}
