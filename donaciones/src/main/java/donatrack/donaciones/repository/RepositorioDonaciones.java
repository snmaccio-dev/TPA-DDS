package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
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

    public List<Donacion> deDonante(Donante donante) {
        return createQuery(
            "select d from Donacion d where d.donante = :donante",
            Donacion.class)
            .setParameter("donante", donante)
            .getResultList();
    }

    public List<Donacion> deDonanteEnEstado(Donante donante, EstadosPosiblesDonacion estado) {
        return createQuery(
            "select d from Donacion d where d.donante = :donante and d.estado.estado = :estado",
            Donacion.class)
            .setParameter("donante", donante)
            .setParameter("estado", estado)
            .getResultList();
    }

    public List<Donacion> deBeneficiaria(Beneficiaria beneficiaria) {
        return createQuery(
            "select d from Donacion d where d.destinatarioAsignado = :beneficiaria",
            Donacion.class)
            .setParameter("beneficiaria", beneficiaria)
            .getResultList();
    }

    public List<Donacion> deBeneficiariaEnEstado(Beneficiaria beneficiaria, EstadosPosiblesDonacion estado) {
        return createQuery(
            "select d from Donacion d "
                + "where d.destinatarioAsignado = :beneficiaria and d.estado.estado = :estado",
            Donacion.class)
            .setParameter("beneficiaria", beneficiaria)
            .setParameter("estado", estado)
            .getResultList();
    }

    public boolean tieneDonacionesDe(Donante donante) {
        if (donante == null || donante.getId() == null) {
            return false;
        }
        return createQuery(
            "select count(d) from Donacion d where d.donante = :donante",
            Long.class)
            .setParameter("donante", donante)
            .getSingleResult() > 0;
    }

    public boolean tieneDonacionesAsignadasA(Beneficiaria beneficiaria) {
        if (beneficiaria == null || beneficiaria.getId() == null) {
            return false;
        }
        return createQuery(
            "select count(d) from Donacion d where d.destinatarioAsignado = :beneficiaria",
            Long.class)
            .setParameter("beneficiaria", beneficiaria)
            .getSingleResult() > 0;
    }
}
