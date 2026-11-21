package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
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
import java.util.Optional;

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
    return calcularPropuesta(donacion).getCandidatas();
  }



public ResultadoCorrida ejecutarMatchmakingProgramado() {
    List<Donacion> pendientes =
        repositorioDonaciones.enEstado(EstadosPosiblesDonacion.EN_DEPOSITO);

    int procesadas = 0;
    List<String> fallidas = new ArrayList<>();

    for (Donacion donacion : pendientes) {
      try {
        Optional<PropuestaAsignacion> existente =
            repositorioPropuestas.buscarPorDonacion(donacion.getId());

        // Una propuesta guardada antes de registrar algoritmo y posicion se recalcula:
        // sin esos datos la pantalla no puede mostrar las dos ejecuciones.
        if (existente.isPresent() && !existente.get().esDeFormatoAnterior()) {
          continue;
        }

        PropuestaAsignacion propuesta = calcularPropuesta(donacion);

        withTransaction(() -> {
          existente.ifPresent(anterior -> repositorioPropuestas.eliminar(donacion.getId()));
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

      // El admin elige el destino a partir del resultado de los algoritmos: la entidad
      // tiene que estar entre las candidatas de la propuesta de esta donacion.
      PropuestaAsignacion propuesta = repositorioPropuestas.buscarPorDonacion(donacionId)
          .orElseThrow(() -> new DatosInvalidosException(
              "No se puede asignar la donación " + donacionId
                  + ": todavía no tiene una propuesta de asignación generada."
          ));

      if (!propuesta.esCandidata(destinatario)) {
        throw new DatosInvalidosException(
            "No se puede asignar la donación: la beneficiaria seleccionada no está "
                + "entre las candidatas propuestas por los algoritmos para esta donación."
        );
      }

      boolean tieneNecesidadCompatible = destinatario.getNecesidades().stream()
          .anyMatch(necesidad ->
              necesidad.getSubcategoria().equals(donacion.getSubcategoria())
          );

      if (!tieneNecesidadCompatible) {
        throw new DatosInvalidosException(
            "No se puede asignar la donación: la beneficiaria seleccionada "
                + "no tiene una necesidad compatible con su subcategoría."
        );
      }

      withTransaction(() -> {
        donacion.confirmarDestino(destinatario);
        repositorioPropuestas.eliminar(donacionId);
      });
  }


  private PropuestaAsignacion calcularPropuesta(Donacion donacion) {
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

    return PropuestaAsignacion.desdeRankings(
        donacion.getId(), rankingCompatibilidad, rankingPrioridad, LocalDateTime.now());
  }

}
