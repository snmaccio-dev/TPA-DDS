package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.service.FiltroDonaciones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
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
        return createQuery("select d from Donacion d order by d.id desc", Donacion.class).getResultList();
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
            "select d from Donacion d where d.estado.estado = :estado order by d.id desc",
            Donacion.class)
            .setParameter("estado", estado)
            .getResultList();
    }

    // Unica query de listado con filtros opcionales. Cualquier parametro null
    // (o lista vacia) no agrega condicion.
    public List<Donacion> buscar(FiltroDonaciones filtro,
                                 Donante donanteOpcional,
                                 Beneficiaria beneficiariaOpcional) {
        StringBuilder jpql = new StringBuilder("select d from Donacion d");
        List<String> condiciones = new ArrayList<>();
        if (donanteOpcional != null) {
            condiciones.add("d.donante = :donante");
        }
        if (beneficiariaOpcional != null) {
            condiciones.add("d.destinatarioAsignado = :beneficiaria");
        }
        EstadosPosiblesDonacion estado = filtro == null ? null : filtro.estado();
        String categoria = filtro == null ? null : filtro.categoria();
        List<String> subcategorias = filtro == null ? null : filtro.subcategorias();
        boolean tieneSubcategorias = subcategorias != null && !subcategorias.isEmpty();
        if (estado != null) {
            condiciones.add("d.estado.estado = :estado");
        }
        if (categoria != null) {
            condiciones.add("d.subcategoria.categoria.nombre = :categoria");
        }
        if (tieneSubcategorias) {
            condiciones.add("d.subcategoria.nombre in :subcategorias");
        }
        if (!condiciones.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", condiciones));
        }
        jpql.append(" order by d.id desc");

        TypedQuery<Donacion> query = createQuery(jpql.toString(), Donacion.class);
        if (donanteOpcional != null) {
            query.setParameter("donante", donanteOpcional);
        }
        if (beneficiariaOpcional != null) {
            query.setParameter("beneficiaria", beneficiariaOpcional);
        }
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (categoria != null) {
            query.setParameter("categoria", categoria);
        }
        if (tieneSubcategorias) {
            query.setParameter("subcategorias", subcategorias);
        }
        return query.getResultList();
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
