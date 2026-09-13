package donatrack.donaciones.service;

import donatrack.donaciones.domain.donacion.asignacion.Algoritmo;
import donatrack.donaciones.domain.donacion.asignacion.CompatibilidadSemantica;
import donatrack.donaciones.domain.donacion.asignacion.PrioridadSubatendidos;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Administrador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPropuestas;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GestorAsignaciones {

  private final Algoritmo compatibilidad;
  private final Algoritmo prioridadSubAtendidos;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioEntidades repositorioEntidades;
  private final RepositorioPropuestas repositorioPropuestas;

  public GestorAsignaciones(RepositorioPropuestas repositorioPropuestas) {
    this.compatibilidad = new CompatibilidadSemantica();
    this.prioridadSubAtendidos = new PrioridadSubatendidos();
    this.repositorioDonaciones = RepositorioDonaciones.getInstance();
    this.repositorioEntidades = RepositorioEntidades.getInstance();
    this.repositorioPropuestas = repositorioPropuestas;
  }

  // EJECUCIÓN A DEMANDA
  public List<Beneficiaria> ejecutarAsignacion(Donacion donacion) {
    return obtenerPropuesta(donacion);
  }

  // Obtiene la propuesta/ranking a partir de todas las beneficiarias registradas
  public List<Beneficiaria> obtenerPropuesta(Donacion donacion) {
    return calcularPropuesta(donacion).candidatas();
  }

  // EJECUCIÓN PROGRAMADA
  public int ejecutarMatchmakingProgramado() {
    List<Donacion> pendientes = repositorioDonaciones.todas().stream()
        .filter(d -> d.getEstado().getEstado() == EstadosPosiblesDonacion.EN_DEPOSITO)
        .toList();

    int procesadas = 0;
    for (Donacion donacion : pendientes) {
      try {
        ResultadoMatchmaking resultado = calcularPropuesta(donacion);
        PropuestaAsignacion propuesta = new PropuestaAsignacion(
            donacion.getId(),
            resultado.candidatas(),
            LocalDateTime.now(),
            resultado.huboCoincidencias()
        );
        repositorioPropuestas.guardar(propuesta);
        procesadas++;
      } catch (RuntimeException e) {
        System.err.println(
            "[MATCHMAKING] Falló para donación " + donacion.getId() + ": " + e.getMessage()
        );
      }
    }
    return procesadas;
  }

  public Optional<PropuestaAsignacion> buscarPropuesta(long donacionId) {
    return repositorioPropuestas.buscarPorDonacion(donacionId);
  }

  public List<PropuestaAsignacion> propuestas() {
    return repositorioPropuestas.todas();
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

  private ResultadoMatchmaking calcularPropuesta(Donacion donacion) {
    if (donacion.getEstado().getEstado() != EstadosPosiblesDonacion.EN_DEPOSITO) {
      throw new IllegalStateException(
          "Solo pueden asignarse donaciones en estado En Depósito."
      );
    }

    List<Beneficiaria> beneficiarias = obtenerBeneficiariasRegistradas();

    List<Beneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, beneficiarias);

    List<Beneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, beneficiarias);

    return filtrarCoincidencias(rankingCompatibilidad, rankingPrioridad);
  }

  private List<Beneficiaria> obtenerBeneficiariasRegistradas() {
    return repositorioEntidades.todas();
  }

  private ResultadoMatchmaking filtrarCoincidencias(
      List<Beneficiaria> ranking1,
      List<Beneficiaria> ranking2) {

    List<Beneficiaria> coincidencias =
        ranking1.stream()
            .filter(ranking2::contains)
            .collect(Collectors.toList());

    if (!coincidencias.isEmpty()) {
      return new ResultadoMatchmaking(coincidencias, true);
    }

    List<Beneficiaria> resultado = new ArrayList<>(ranking1);
    ranking2.stream()
        .filter(beneficiaria -> !resultado.contains(beneficiaria))
        .forEach(resultado::add);

    return new ResultadoMatchmaking(resultado, false);
  }

  private record ResultadoMatchmaking(List<Beneficiaria> candidatas, boolean huboCoincidencias) {}
}