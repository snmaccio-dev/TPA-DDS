package donatrack.donaciones.repository;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.EstadoEnvio;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioNotificaciones implements WithSimplePersistenceUnit {

    private static final int TAMANIO_MAXIMO = 100;
    private static final int TAMANIO_POR_DEFECTO = 20;

    // guardar funciona con o sin transaccion abierta: si hay una activa
    // (porque el gestor ya abrio una en withTransaction), solo persiste; si no,
    // abre y commitea su propia. Importante porque GestorNotificaciones se
    // invoca desde dentro de withTransaction del observer y tambien desde
    // flujos sin transaccion.
    public void guardar(Notificacion notificacion) {
        if (entityManager().getTransaction().isActive()) {
            persist(notificacion);
        } else {
            withTransaction(() -> persist(notificacion));
        }
    }

    public Optional<Notificacion> buscarPorId(long id) {
        return Optional.ofNullable(find(Notificacion.class, id));
    }

    public List<Notificacion> deDonante(Donante donante,
                                        boolean soloNoLeidas,
                                        int pagina,
                                        int tamanio) {
        if (donante == null) {
            return List.of();
        }
        String base = "select n from Notificacion n where n.donante = :donante";
        if (soloNoLeidas) {
            base += " and n.fechaHoraLectura is null";
        }
        base += " order by n.fechaHoraCreacion desc";
        TypedQuery<Notificacion> query = createQuery(base, Notificacion.class)
            .setParameter("donante", donante);
        return paginar(query, pagina, tamanio);
    }

    public List<Notificacion> deBeneficiaria(Beneficiaria beneficiaria,
                                             boolean soloNoLeidas,
                                             int pagina,
                                             int tamanio) {
        if (beneficiaria == null) {
            return List.of();
        }
        String base = "select n from Notificacion n where n.beneficiaria = :beneficiaria";
        if (soloNoLeidas) {
            base += " and n.fechaHoraLectura is null";
        }
        base += " order by n.fechaHoraCreacion desc";
        TypedQuery<Notificacion> query = createQuery(base, Notificacion.class)
            .setParameter("beneficiaria", beneficiaria);
        return paginar(query, pagina, tamanio);
    }

    public List<Notificacion> deAdministradores(boolean soloNoLeidas, int pagina, int tamanio) {
        String base = "select n from Notificacion n where n.paraAdministradores = true";
        if (soloNoLeidas) {
            base += " and n.fechaHoraLectura is null";
        }
        base += " order by n.fechaHoraCreacion desc";
        TypedQuery<Notificacion> query = createQuery(base, Notificacion.class);
        return paginar(query, pagina, tamanio);
    }

    public long cantidadNoLeidasDeDonante(Donante donante) {
        if (donante == null) {
            return 0L;
        }
        return createQuery(
            "select count(n) from Notificacion n "
                + "where n.donante = :donante and n.fechaHoraLectura is null",
            Long.class)
            .setParameter("donante", donante)
            .getSingleResult();
    }

    public long cantidadNoLeidasDeBeneficiaria(Beneficiaria beneficiaria) {
        if (beneficiaria == null) {
            return 0L;
        }
        return createQuery(
            "select count(n) from Notificacion n "
                + "where n.beneficiaria = :beneficiaria and n.fechaHoraLectura is null",
            Long.class)
            .setParameter("beneficiaria", beneficiaria)
            .getSingleResult();
    }

    public long cantidadNoLeidasAdministradores() {
        return createQuery(
            "select count(n) from Notificacion n "
                + "where n.paraAdministradores = true and n.fechaHoraLectura is null",
            Long.class)
            .getSingleResult();
    }

    public List<Notificacion> todas(EstadoEnvio estadoEnvio,
                                    TipoNotificacion tipo,
                                    int pagina,
                                    int tamanio) {
        StringBuilder jpql = new StringBuilder("select n from Notificacion n");
        List<String> condiciones = new ArrayList<>();
        if (estadoEnvio != null) {
            condiciones.add("n.estadoEnvio = :estadoEnvio");
        }
        if (tipo != null) {
            condiciones.add("n.tipo = :tipo");
        }
        if (!condiciones.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", condiciones));
        }
        jpql.append(" order by n.fechaHoraCreacion desc");

        TypedQuery<Notificacion> query = createQuery(jpql.toString(), Notificacion.class);
        if (estadoEnvio != null) {
            query.setParameter("estadoEnvio", estadoEnvio);
        }
        if (tipo != null) {
            query.setParameter("tipo", tipo);
        }
        return paginar(query, pagina, tamanio);
    }

    // === Marcar leidas en bloque (requieren transaccion abierta) ===

    public int marcarLeidasDeDonante(Donante donante, LocalDateTime fechaHora) {
        if (donante == null) {
            return 0;
        }
        return createQuery(
            "update Notificacion n set n.fechaHoraLectura = :fecha "
                + "where n.donante = :donante and n.fechaHoraLectura is null")
            .setParameter("fecha", fechaHora)
            .setParameter("donante", donante)
            .executeUpdate();
    }

    public int marcarLeidasDeBeneficiaria(Beneficiaria beneficiaria, LocalDateTime fechaHora) {
        if (beneficiaria == null) {
            return 0;
        }
        return createQuery(
            "update Notificacion n set n.fechaHoraLectura = :fecha "
                + "where n.beneficiaria = :beneficiaria and n.fechaHoraLectura is null")
            .setParameter("fecha", fechaHora)
            .setParameter("beneficiaria", beneficiaria)
            .executeUpdate();
    }

    public int marcarLeidasDeAdministradores(LocalDateTime fechaHora) {
        return createQuery(
            "update Notificacion n set n.fechaHoraLectura = :fecha "
                + "where n.paraAdministradores = true and n.fechaHoraLectura is null")
            .setParameter("fecha", fechaHora)
            .executeUpdate();
    }

    // === Delete en bloque (requieren transaccion abierta) ===

    public int eliminarDeDonacion(Donacion donacion) {
        if (donacion == null || donacion.getId() == null) {
            return 0;
        }
        return createQuery("delete from Notificacion n where n.donacion = :donacion")
            .setParameter("donacion", donacion)
            .executeUpdate();
    }

    public int eliminarDeDonante(Donante donante) {
        if (donante == null || donante.getId() == null) {
            return 0;
        }
        return createQuery("delete from Notificacion n where n.donante = :donante")
            .setParameter("donante", donante)
            .executeUpdate();
    }

    public int eliminarDeBeneficiaria(Beneficiaria beneficiaria) {
        if (beneficiaria == null || beneficiaria.getId() == null) {
            return 0;
        }
        return createQuery("delete from Notificacion n where n.beneficiaria = :beneficiaria")
            .setParameter("beneficiaria", beneficiaria)
            .executeUpdate();
    }

    // Paginacion con limites defensivos: pagina mayor o igual a 0, tamanio
    // entre 1 y 100. Un pedido fuera de rango se recorta en vez de 400,
    // asi un parametro feo del front no rompe la UI.
    private List<Notificacion> paginar(TypedQuery<Notificacion> query, int pagina, int tamanio) {
        int tamanioEfectivo = tamanio <= 0 ? TAMANIO_POR_DEFECTO : Math.min(tamanio, TAMANIO_MAXIMO);
        int paginaEfectiva = Math.max(0, pagina);
        return query
            .setFirstResult(paginaEfectiva * tamanioEfectivo)
            .setMaxResults(tamanioEfectivo)
            .getResultList();
    }
}
