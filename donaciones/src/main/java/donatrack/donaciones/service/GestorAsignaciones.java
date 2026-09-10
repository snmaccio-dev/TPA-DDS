package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.asignacion.Algoritmo;
import donatrack.donaciones.domain.donacion.asignacion.CompatibilidadSemantica;
import donatrack.donaciones.domain.donacion.asignacion.PrioridadSubatendidos;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Administrador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioPersonas;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GestorAsignaciones {

  private final Algoritmo compatibilidad;
  private final Algoritmo prioridadSubAtendidos;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioPersonas repositorioPersonas;

  public GestorAsignaciones() {
    this.compatibilidad = new CompatibilidadSemantica();
    this.prioridadSubAtendidos = new PrioridadSubatendidos();
    this.repositorioDonaciones = RepositorioDonaciones.getInstance();
    this.repositorioPersonas = RepositorioPersonas.getInstance();
  }

  // EJECUCIÓN A DEMANDA
  public List<Beneficiaria> ejecutarAsignacion(Donacion donacion) {
    return obtenerPropuesta(donacion);
  }

  // Obtiene la propuesta/ranking a partir de todas las beneficiarias registradas
  public List<Beneficiaria> obtenerPropuesta(Donacion donacion) {
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException(
          "Solo pueden asignarse donaciones en estado En Depósito."
      );
    }

    List<Beneficiaria> beneficiarias = obtenerBeneficiariasRegistradas();

    List<Beneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, beneficiarias);

    List<Beneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, beneficiarias);

    return filtrarCoincidencias(
        rankingCompatibilidad,
        rankingPrioridad
    );
  }

  // Confirma el destino final de la donación validando administrador y donación
  public void confirmarDestino(long donacionId,
                               Beneficiaria destinatario,
                               Administrador administrador) {
    if (administrador == null) {
      throw new IllegalArgumentException(
          "Debe indicarse el administrador que confirma el destino."
      );
    }
    if (destinatario == null) {
      throw new IllegalArgumentException(
          "Debe indicarse la beneficiaria a confirmar como destinataria."
      );
    }
    Donacion donacion = repositorioDonaciones.buscarPorId(donacionId)
        .orElseThrow(() -> new IllegalArgumentException(
            "No existe la donación con ID " + donacionId
        ));
    donacion.confirmarDestino(destinatario);
  }

  private List<Beneficiaria> obtenerBeneficiariasRegistradas() {
    return repositorioPersonas.todos().stream()
        .flatMap(persona -> persona.comoRol(Beneficiaria.class).stream())
        .toList();
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
