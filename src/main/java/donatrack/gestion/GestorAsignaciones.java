package donatrack.gestion;

import donatrack.model.donacion.asignacion.Algoritmo;
import donatrack.model.donacion.asignacion.CompatibilidadSemantica;
import donatrack.model.donacion.asignacion.PrioridadSubatendidos;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.model.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GestorAsignaciones {

  private final Algoritmo compatibilidad;
  private final Algoritmo prioridadSubAtendidos;

  public GestorAsignaciones() {
    this.compatibilidad = new CompatibilidadSemantica();
    this.prioridadSubAtendidos = new PrioridadSubatendidos();
  }

  // EJECUCIÓN A DEMANDA
  public List<EntidadBeneficiaria> ejecutarAsignacion(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    return obtenerPropuesta(donacion, entidades);
  }

  // Obtiene la propuesta/ranking
  public List<EntidadBeneficiaria> obtenerPropuesta(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    if (!(donacion.getEstado() instanceof EnDeposito)) {
      throw new IllegalStateException(
          "Solo pueden asignarse donaciones en estado En Depósito."
      );
    }

    List<EntidadBeneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, entidades);

    List<EntidadBeneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, entidades);

    return filtrarCoincidencias(
        rankingCompatibilidad,
        rankingPrioridad
    );
  }

  private List<EntidadBeneficiaria> filtrarCoincidencias(
      List<EntidadBeneficiaria> ranking1,
      List<EntidadBeneficiaria> ranking2) {

    List<EntidadBeneficiaria> coincidencias =
        ranking1.stream()
            .filter(ranking2::contains)
            .collect(Collectors.toList());

    if (!coincidencias.isEmpty()) {
      return coincidencias;
    }

    List<EntidadBeneficiaria> resultado =
        new ArrayList<>(ranking1);

    ranking2.stream()
        .filter(entidad -> !resultado.contains(entidad))
        .forEach(resultado::add);

    return resultado;
  }
}
