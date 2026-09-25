package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioPropuestas implements WithSimplePersistenceUnit {

    public void guardar(PropuestaAsignacion propuesta) {
        persist(propuesta);
    }

    public Optional<PropuestaAsignacion> buscarPorDonacion(long donacionId) {
        return createQuery(
            "select p from PropuestaAsignacion p where p.donacionId = :donacionId",
            PropuestaAsignacion.class)
            .setParameter("donacionId", donacionId)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<PropuestaAsignacion> todas() {
        return createQuery("select p from PropuestaAsignacion p", PropuestaAsignacion.class)
            .getResultList();
    }

    public void eliminar(long donacionId) {
        buscarPorDonacion(donacionId).ifPresent(this::remove);
    }
}
