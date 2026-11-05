package donatrack.logistica.repository;

import donatrack.logistica.domain.ruta.EstadoRuta;
import donatrack.logistica.domain.ruta.RutaReparto;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RepositorioRutas implements WithSimplePersistenceUnit {

  private static RepositorioRutas instancia;

  private RepositorioRutas() {
  }

  public static RepositorioRutas getInstance() {
    if (instancia == null) {
      instancia = new RepositorioRutas();
    }

    return instancia;
  }

  public void guardar(RutaReparto ruta) {
    persist(ruta);
  }

  public void guardar(List<RutaReparto> nuevasRutas) {
    nuevasRutas.forEach(this::persist);
  }

  public List<RutaReparto> todas() {
    return createQuery("select r from RutaReparto r", RutaReparto.class).getResultList();
  }

  public Optional<RutaReparto> buscarPorId(long id) {
    return Optional.ofNullable(find(RutaReparto.class, id));
  }

  public List<RutaReparto> enCurso() {
    return createQuery(
        "select r from RutaReparto r where r.estado = :estado",
        RutaReparto.class)
        .setParameter("estado", EstadoRuta.EN_CURSO)
        .getResultList();
  }

  public List<RutaReparto> enCursoDeBeneficiaria(long beneficiariaId) {
    return createQuery(
        "select distinct r from RutaReparto r join r.entregas e "
            + "where r.estado = :estado and e.beneficiariaId = :beneficiariaId",
        RutaReparto.class)
        .setParameter("estado", EstadoRuta.EN_CURSO)
        .setParameter("beneficiariaId", beneficiariaId)
        .getResultList();
  }

  public List<RutaReparto> enCursoDeDonaciones(List<Long> donacionIds) {
    if (donacionIds == null || donacionIds.isEmpty()) {
      return List.of();
    }

    return createQuery(
        "select distinct r from RutaReparto r join r.entregas e "
            + "where r.estado = :estado and e.donacionId in :donacionIds",
        RutaReparto.class)
        .setParameter("estado", EstadoRuta.EN_CURSO)
        .setParameter("donacionIds", donacionIds)
        .getResultList();
  }

  public Set<String> patentesConRutaEnCurso() {
    return new HashSet<>(createQuery(
        "select distinct r.camion.patente from RutaReparto r where r.estado = :estado",
        String.class)
        .setParameter("estado", EstadoRuta.EN_CURSO)
        .getResultList());
  }

  public boolean tieneRutasDeCamion(String patente) {
    return createQuery(
        "select count(r) from RutaReparto r where r.camion.patente = :patente",
        Long.class)
        .setParameter("patente", patente)
        .getSingleResult() > 0;
  }

  public void eliminar(long id) {
    buscarPorId(id).ifPresent(this::remove);
  }
}
