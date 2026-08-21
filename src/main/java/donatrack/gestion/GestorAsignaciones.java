package donatrack.gestion;

import donatrack.model.donacion.asignacion.Algoritmo;
import donatrack.model.donacion.asignacion.CompatibilidadSemantica;
import donatrack.model.donacion.asignacion.PrioridadSubatendidos;
import donatrack.model.donacion.estado.EnDeposito;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

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
  public List<Beneficiaria> ejecutarAsignacion(
      Donacion donacion,
      List<Beneficiaria> beneficiarias) {

    return obtenerPropuesta(donacion, beneficiarias);
  }

  // Obtiene la propuesta/ranking
  public List<Beneficiaria> obtenerPropuesta(
      Donacion donacion,
      List<Beneficiaria> beneficiarias) {

    if (!(donacion.getEstado() instanceof EnDeposito)) {
      throw new IllegalStateException(
          "Solo pueden asignarse donaciones en estado En Depósito."
      );
    }

    List<Beneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, beneficiarias);

    List<Beneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, beneficiarias);

    return filtrarCoincidencias(
        rankingCompatibilidad,
        rankingPrioridad
    );
  }

  private List<Beneficiaria> filtrarCoincidencias(
      List<Beneficiaria> ranking1,
      List<Beneficiaria> ranking2) {

    List<Beneficiaria> coincidencias =
        ranking1.stream()
            .filter(ranking2::contains)
            .collect(Collectors.toList());

    if (!coincidencias.isEmpty()) {
      return coincidencias;
    }

    List<Beneficiaria> resultado =
        new ArrayList<>(ranking1);

    ranking2.stream()
        .filter(beneficiaria -> !resultado.contains(beneficiaria))
        .forEach(resultado::add);

    return resultado;
  }
}
