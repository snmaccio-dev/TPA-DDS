package donatrack.model.donacion.asignacion;

import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

public interface Algoritmo {
  List<Beneficiaria> matchmaking(Donacion donacion, List<Beneficiaria> beneficiarias);
}
