package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioDonaciones implements WithSimplePersistenceUnit {

    private static RepositorioDonaciones instancia;

    private RepositorioDonaciones() {}

    public static RepositorioDonaciones getInstance() {
        if (instancia == null) {
            instancia = new RepositorioDonaciones();
        }
        return instancia;
    }

    public void guardar(Donacion donacion) {
        persist(donacion);
    }

    public List<Donacion> todas() {
        return createQuery("select d from Donacion d", Donacion.class).getResultList();
    }

    public Optional<Donacion> buscarPorId(Long id) {
        return Optional.ofNullable(find(Donacion.class, id));
    }

    public void eliminar(Long id) {
        buscarPorId(id).ifPresent(this::remove);
    }

    public int cantidad() {
        return createQuery("select count(d) from Donacion d", Long.class)
            .getSingleResult()
            .intValue();
    }

    public List<Donacion> enEstado(EstadosPosiblesDonacion estado) {
        return createQuery(
            "select d from Donacion d where d.estado.estado = :estado",
            Donacion.class)
            .setParameter("estado", estado)
            .getResultList();
    }
}
