package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.necesidad.NecesidadRecurrente;
import donatrack.donaciones.domain.necesidad.Periodo;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioNecesidades;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;

public class NecesidadesService implements WithSimplePersistenceUnit {

  private final RepositorioNecesidades repositorio;
  private final DonacionesService donacionesService;

  public NecesidadesService(RepositorioNecesidades repositorio,
                            DonacionesService donacionesService) {
    this.repositorio = repositorio;
    this.donacionesService = donacionesService;
  }

  public Necesidad crear(Beneficiaria beneficiaria,
                         String tipo,
                         String descripcion,
                         int cantidad,
                         String subcategoria,
                         String periodo) {
    boolean recurrente = esRecurrente(tipo);
    Periodo periodoParseado = recurrente ? periodoDe(periodo) : null;
    Subcategoria sub = donacionesService.subcategoriaPorNombre(subcategoria);
    Necesidad necesidad = recurrente
        ? new NecesidadRecurrente(descripcion, cantidad, sub, beneficiaria, periodoParseado)
        : new NecesidadExtraordinaria(descripcion, cantidad, sub, beneficiaria);
    withTransaction(() -> repositorio.guardar(necesidad));
    return necesidad;
  }

  public List<Necesidad> todas() {
    return repositorio.todas();
  }

  public List<Necesidad> deBeneficiaria(Beneficiaria beneficiaria) {
    return repositorio.deBeneficiaria(beneficiaria);
  }

  public Necesidad buscar(long id) {
    return repositorio.buscarPorId(id)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe la necesidad con ID "
                    + id
            ));
  }

  public void eliminar(long id) {
    buscar(id);
    withTransaction(() -> repositorio.eliminar(id));
  }

  public void actualizar(long id, String descripcion, int cantidad, String subcategoria) {
    Subcategoria sub = donacionesService.subcategoriaPorNombre(subcategoria);
    withTransaction(() -> {
      Necesidad necesidad = buscar(id);
      necesidad.setDescripcion(descripcion);
      necesidad.setCantidad(cantidad);
      necesidad.setSubcategoria(sub);
    });
  }

  private static boolean esRecurrente(String tipo) {
    if ("RECURRENTE".equalsIgnoreCase(tipo)) {
      return true;
    }
    if ("EXTRAORDINARIA".equalsIgnoreCase(tipo)) {
      return false;
    }
    throw new DatosInvalidosException(
        "El tipo de necesidad debe ser EXTRAORDINARIA o RECURRENTE (recibido: " + tipo + ")."
    );
  }

  private static Periodo periodoDe(String periodo) {
    if (periodo == null) {
      return null;
    }
    try {
      return Periodo.valueOf(periodo);
    } catch (IllegalArgumentException e) {
      throw new DatosInvalidosException("El periodo '" + periodo + "' no es valido.");
    }
  }
}
