package donatrack.donaciones.domain.donacion.asignacion;

import java.util.List;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

public interface Algoritmo {
  List<Beneficiaria> matchmaking(Donacion donacion, List<Beneficiaria> beneficiarias);
}
