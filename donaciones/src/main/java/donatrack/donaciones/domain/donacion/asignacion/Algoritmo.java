package donatrack.donaciones.domain.donacion.asignacion;

import java.util.List;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

public interface Algoritmo {

  int MAXIMO_DE_CANDIDATAS = 10;

  // candidatas ya viene filtrada por AsignacionesService: solo entidades con al menos una
  // necesidad de la subcategoria de la donacion (Beneficiaria.tieneNecesidadDe). Cada
  // algoritmo las ordena con su criterio y devuelve hasta MAXIMO_DE_CANDIDATAS.
  List<Beneficiaria> matchmaking(Donacion donacion, List<Beneficiaria> candidatas);
}
