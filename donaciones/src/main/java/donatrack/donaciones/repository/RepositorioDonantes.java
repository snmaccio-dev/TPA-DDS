package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import donatrack.donaciones.domain.contacto.TipoContacto;

import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RepositorioDonantes implements WithSimplePersistenceUnit {

    public void guardar(Donante donante) {
        persist(donante);
    }

    public void eliminar(Donante donante) {
        if (donante == null) {
            return;
        }
        remove(donante);
    }

    public Optional<Donante> buscarPorPersona(Persona persona) {
        return createQuery(
            "select d from Donante d where d.persona = :persona",
            Donante.class)
            .setParameter("persona", persona)
            .getResultList()
            .stream()
            .findFirst();
    }

    public Optional<Donante> buscarPorDocumento(String documento) {
        Optional<Donante> porHumana = createQuery(
            "select d from Donante d, PersonaHumana p "
                + "where d.persona = p and p.documento = :documento",
            Donante.class)
            .setParameter("documento", documento)
            .getResultList()
            .stream()
            .findFirst();
        if (porHumana.isPresent()) {
            return porHumana;
        }
        return createQuery(
            "select d from Donante d, PersonaJuridica p "
                + "where d.persona = p and p.cuit = :documento",
            Donante.class)
            .setParameter("documento",
                donatrack.donaciones.domain.persona.PersonaJuridica.normalizarCuit(documento))
            .getResultList()
            .stream()
            .findFirst();
    }

    // === Listado paginado ===
    //
    // Tres consultas por pagina, sin N+1 y paginando en la base:
    //  1. count de los donantes que cumplen el filtro;
    //  2. ids de la pagina pedida (setFirstResult/setMaxResults sobre una consulta sin
    //     colecciones, asi Hibernate pagina en SQL y no en memoria);
    //  3. esos donantes con persona, contactos y contacto predeterminado en un solo
    //     fetch join (ahi si hay una coleccion, pero ya no se pagina).
    // Las subclases de Persona se unen con un join de entidad para poder filtrar y
    // ordenar por nombre, razon social, documento o CUIT.

    private static final String DESDE =
        "from Donante d join d.persona p "
            + "left join PersonaHumana h on h.id = p.id "
            + "left join PersonaJuridica j on j.id = p.id ";

    private static final String FILTRO =
        "where (lower(h.nombre) like :texto escape '!' "
            + "or lower(h.apellido) like :texto escape '!' "
            + "or lower(concat(h.nombre, ' ', h.apellido)) like :texto escape '!' "
            + "or lower(j.razonSocial) like :texto escape '!' "
            + "or lower(h.documento) like :digitos escape '!' "
            + "or j.cuit like :digitos escape '!' "
            + "or exists (select m.id from MedioContacto m "
            + "           where m.persona = p and m.tipo = :tipoEmail "
            + "           and lower(m.valor) like :texto escape '!')) ";

    private static final String ORDEN =
        "order by lower(coalesce(j.razonSocial, concat(h.nombre, ' ', h.apellido))), d.id";

    public long contar(String busqueda) {
        TypedQuery<Long> query = createQuery(
            "select count(d) " + DESDE + filtroDe(busqueda), Long.class);
        conFiltro(query, busqueda);
        return query.getSingleResult();
    }

    // pagina empieza en 1. Devuelve los donantes en orden de nombre o razon social.
    public List<Donante> pagina(String busqueda, int pagina, int tamanio) {
        TypedQuery<Long> consultaIds = createQuery(
            "select d.id " + DESDE + filtroDe(busqueda) + ORDEN, Long.class);
        conFiltro(consultaIds, busqueda);
        List<Long> ids = consultaIds
            .setFirstResult((pagina - 1) * tamanio)
            .setMaxResults(tamanio)
            .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Donante> porId = createQuery(
            "select distinct d from Donante d "
                + "join fetch d.persona p "
                + "left join fetch p.contactos "
                + "left join fetch p.contactoPredeterminado "
                + "where d.id in :ids",
            Donante.class)
            .setParameter("ids", ids)
            .setHint("hibernate.query.passDistinctThrough", false)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(Donante::getId, Function.identity()));

        // El "in" no respeta el orden: se rearma con el de la consulta de ids.
        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }

    private static boolean hayBusqueda(String busqueda) {
        return busqueda != null && !busqueda.isBlank();
    }

    private static String filtroDe(String busqueda) {
        return hayBusqueda(busqueda) ? FILTRO : "";
    }

    private static void conFiltro(TypedQuery<?> query, String busqueda) {
        if (!hayBusqueda(busqueda)) {
            return;
        }
        String texto = busqueda.trim().toLowerCase(Locale.ROOT);
        // El CUIT se guarda solo con digitos: "30-70000003-3" tiene que encontrarlo. Solo
        // se le sacan los separadores si lo buscado es un numero; si no, un texto como
        // "ana 123" terminaria buscando "123" en todos los CUIT.
        String soloDigitos = texto.replaceAll("\\D", "");
        String cuit = texto.matches("[\\d\\s.\\-]+") && !soloDigitos.isEmpty()
            ? soloDigitos
            : texto;
        query.setParameter("texto", "%" + escaparLike(texto) + "%");
        query.setParameter("digitos", "%" + escaparLike(cuit) + "%");
        query.setParameter("tipoEmail", TipoContacto.EMAIL);
    }

    private static String escaparLike(String valor) {
        return valor.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    public List<Donante> queRequierenAvisoDeInactividad(LocalDate limite) {
        return createQuery(
            "select d from Donante d "
                + "where d.ultimaInteraccion < :limite "
                + "and (d.fechaUltimoAvisoInactividad is null "
                + "     or d.fechaUltimoAvisoInactividad < d.ultimaInteraccion)",
            Donante.class)
            .setParameter("limite", limite)
            .getResultList();
    }

    ////////////////////////////////

    public List<Donante> buscarPorPersonas(Collection<Persona> personas) {
        if (personas == null || personas.isEmpty()) {
            return List.of();
        }

        return createQuery(
            "select d from Donante d where d.persona in :personas",
            Donante.class)
            .setParameter("personas", personas)
            .getResultList();
        }
}
