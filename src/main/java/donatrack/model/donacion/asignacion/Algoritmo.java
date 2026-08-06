package donatrack.model.donacion.asignacion;

import java.util.List;

import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;

public interface Algoritmo {
  List<EntidadBeneficiaria> matchmaking(Donacion donacion, List<EntidadBeneficiaria> entidades);
}
