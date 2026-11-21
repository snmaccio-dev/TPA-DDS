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

    // Cada corrida (job nocturno o a demanda) recalcula las propuestas de todas las
    // donaciones en deposito: las necesidades y las donaciones recibidas cambian, y una
    // propuesta vieja (por ejemplo, generada antes de filtrar por necesidad) no tiene
    // que quedar.
    for (Donacion donacion : pendientes) {
      try {
        PropuestaAsignacion propuesta = calcularPropuesta(donacion);
        boolean habiaUna =
            repositorioPropuestas.buscarPorDonacion(donacion.getId()).isPresent();

        withTransaction(() -> {
          if (habiaUna) {
            repositorioPropuestas.eliminar(donacion.getId());
          }
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

      // Misma regla que usa el matchmaking para elegir candidatas.
      if (!destinatario.tieneNecesidadDe(donacion.getSubcategoria())) {
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

    // Unico filtro de compatibilidad: solo compiten las entidades con una necesidad de
    // la subcategoria donada. Si no hay ninguna, la propuesta queda sin candidatas.
    List<Beneficiaria> candidatas = candidatasCompatibles(donacion);

    List<Beneficiaria> rankingCompatibilidad =
        compatibilidad.matchmaking(donacion, candidatas);

    List<Beneficiaria> rankingPrioridad =
        prioridadSubAtendidos.matchmaking(donacion, candidatas);

    return PropuestaAsignacion.desdeRankings(
        donacion.getId(), rankingCompatibilidad, rankingPrioridad, LocalDateTime.now());
  }

  private List<Beneficiaria> candidatasCompatibles(Donacion donacion) {
    return repositorioEntidades.todas().stream()
        .filter(beneficiaria -> beneficiaria.tieneNecesidadDe(donacion.getSubcategoria()))
        .toList();
  }

}
