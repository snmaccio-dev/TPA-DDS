package donatrack.donaciones.service;

import donatrack.donaciones.domain.TransicionNoPermitidaException;
import donatrack.donaciones.domain.donacion.asignacion.Algoritmo;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPropuestas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AsignacionesService implements WithSimplePersistenceUnit {

  private final Algoritmo compatibilidad;
  private final Algoritmo prioridadSubAtendidos;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioEntidades repositorioEntidades;
  private final RepositorioPropuestas repositorioPropuestas;
  private final DonacionesService donacionesService;

  public AsignacionesService(Algoritmo compatibilidad,
                             Algoritmo prioridadSubAtendidos,
                             RepositorioDonaciones repositorioDonaciones,
                             RepositorioEntidades repositorioEntidades,
                             RepositorioPropuestas repositorioPropuestas,
                             DonacionesService donacionesService) {
    this.compatibilidad = compatibilidad;
    this.prioridadSubAtendidos = prioridadSubAtendidos;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioEntidades = repositorioEntidades;
    this.repositorioPropuestas = repositorioPropuestas;
    this.donacionesService = donacionesService;
  }

  public List<Beneficiaria> ejecutarAsignacion(long donacionId) {
    Donacion donacion = donacionesService.buscar(donacionId);
    return calcularPropuesta(donacion).candidatas();
  }

/*

  public ResultadoCorrida ejecutarMatchmakingProgramado() {
    List<Donacion> pendientes = repositorioDonaciones.enEstado(EstadosPosiblesDonacion.EN_DEPOSITO);

    int procesadas = 0;
    List<String> fallidas = new ArrayList<>();
    for (Donacion donacion : pendientes) {
      try {
        ResultadoMatchmaking resultado = calcularPropuesta(donacion);
        withTransaction(() -> {
          PropuestaAsignacion propuesta = new PropuestaAsignacion(
              donacion.getId(),
              resultado.candidatas(),
              LocalDateTime.now(),
              resultado.huboCoincidencias()
          );
          repositorioPropuestas.guardar(propuesta);
        });
        procesadas++;
      } catch (RuntimeException e) {
        fallidas.add("donación " + donacion.getId() + ": " + e.getMessage());
      }
    }
    return new ResultadoCorrida(procesadas, fallidas);
  }

*/


public ResultadoCorrida ejecutarMatchmakingProgramado() {
    List<Donacion> pendientes =
        repositorioDonaciones.enEstado(EstadosPosiblesDonacion.EN_DEPOSITO);

    int procesadas = 0;
    List<String> fallidas = new ArrayList<>();

    for (Donacion donacion : pendientes) {
      try {
        if (repositorioPropuestas.buscarPorDonacion(donacion.getId()).isPresent()) {
          continue;
        }

        ResultadoMatchmaking resultado = calcularPropuesta(donacion);

        withTransaction(() -> {
          PropuestaAsignacion propuesta = new PropuestaAsignacion(
              donacion.getId(),
              resultado.candidatas(),
              LocalDateTime.now(),
              resultado.huboCoincidencias()
          );

          repositorioPropuestas.guardar(propuesta);
        });

        procesadas++;
      } catch (RuntimeException e) {
        fallidas.add("donación " + donacion.getId() + ": " + e.getMessage());
      }
    }

    return new ResultadoCorrida(procesadas, fallidas);
}


  public PropuestaAsignacion propuestaDe(long donacionId) {
    return repositorioPropuestas.buscarPorDonacion(donacionId)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No hay propuesta generada para la donación " + donacionId
        ));
  }

  public List<PropuestaAsignacion> propuestas() {
    return repositorioPropuestas.todas();
  }


  public void confirmarDestino(long donacionId, long beneficiariaId) {
      Beneficiaria destinatario = repositorioEntidades.buscarPorId(beneficiariaId)
          .orElseThrow(() -> new RecursoInexistenteException(
              "No existe la beneficiaria con id " + beneficiariaId
          ));

      Donacion donacion = donacionesService.buscar(donacionId);

      boolean tieneNecesidadCompatible = destinatario.getNecesidades().stream()
          .anyMatch(necesidad ->
              necesidad.getSubcategoria().equals(donacion.getSubcategoria())
          );

      if (!tieneNecesidadCompatible) {
        throw new donatrack.donaciones.domain.DatosInvalidosException(
            "No se puede asignar la donación: la beneficiaria seleccionada "
                + "no tiene una necesidad compatible con su subcategoría."
        );
      }

      withTransaction(() -> {
        donacion.confirmarDestino(destinatario);
        repositorioPropuestas.eliminar(donacionId);
      });
  }


  private ResultadoMatchmaking calcularPropuesta(Donacion donacion) {
    if (donacion.getEstado().getEstado() != EstadosPosiblesDonacion.EN_DEPOSITO) {
      throw new TransicionNoPermitidaException(
          "Solo pueden asignarse donaciones en estado En Depósito."
      );
    }

    List<Beneficiaria> beneficiarias = repositorioEntidades.todas();

    List<Beneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, beneficiarias);

    List<Beneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, beneficiarias);

    return filtrarCoincidencias(rankingCompatibilidad, rankingPrioridad);
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
