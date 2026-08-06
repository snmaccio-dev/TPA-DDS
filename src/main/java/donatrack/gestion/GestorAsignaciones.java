package donatrack.gestion;

import donatrack.model.donacion.asignacion.Algoritmo;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.model.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GestorAsignaciones {

  private Algoritmo compatibilidad;
  private Algoritmo prioridadSubAtendidos;

  public List<EntidadBeneficiaria> obtenerPropuesta(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    if (!(donacion.getEstado() instanceof EnDeposito)) {
      throw new IllegalStateException(
          "Solo pueden asignarse donaciones en estado En Depósito.");
    }

    List<EntidadBeneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, entidades);

    List<EntidadBeneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, entidades);

    return filtrarCoincidencias(rankingCompatibilidad, rankingPrioridad);
  }

  private List<EntidadBeneficiaria> filtrarCoincidencias(
      List<EntidadBeneficiaria> ranking1,
      List<EntidadBeneficiaria> ranking2) {

    List<EntidadBeneficiaria> coincidencias = ranking1.stream()
        .filter(ranking2::contains)
        .collect(Collectors.toList());

    if (!coincidencias.isEmpty()) {
      return coincidencias;
    }

    List<EntidadBeneficiaria> resultado = new ArrayList<>(ranking1);

    List<EntidadBeneficiaria> nuevasEntidades = ranking2.stream()
        .filter(entidad -> !resultado.contains(entidad))
        .collect(Collectors.toList());

    resultado.addAll(nuevasEntidades);

    return resultado;
  }
}
