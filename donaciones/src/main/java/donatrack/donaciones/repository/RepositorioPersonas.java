package donatrack.donaciones.repository;

import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioPersonas implements WithSimplePersistenceUnit {

    private static RepositorioPersonas instancia;

    private RepositorioPersonas() {}

    public static RepositorioPersonas getInstance() {
        if (instancia == null) {
            instancia = new RepositorioPersonas();
        }
        return instancia;
    }

    public void guardar(Persona persona) {
        String documento = persona.getDocumento();
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("La persona no tiene documento.");
        }
        persist(persona);
    }

    public Optional<Persona> buscarPorDocumento(String documento) {
        Optional<Persona> humana = createQuery(
            "select p from PersonaHumana p where p.documento = :documento",
            Persona.class)
            .setParameter("documento", documento)
            .getResultList()
            .stream()
            .findFirst();
        if (humana.isPresent()) {
            return humana;
        }
        return createQuery(
            "select p from PersonaJuridica p where p.cuit = :documento",
            Persona.class)
            .setParameter("documento", documento)
            .getResultList()
            .stream()
            .findFirst();
    }

    public Optional<PersonaJuridica> buscarJuridicaPorCuit(String cuit) {
        return createQuery(
            "select p from PersonaJuridica p where p.cuit = :cuit",
            PersonaJuridica.class)
            .setParameter("cuit", cuit)
            .getResultList()
            .stream()
            .findFirst();
    }

    public Optional<Persona> buscarPorEmail(String email) {
        return createQuery(
            "select p from Persona p join p.contactos c "
                + "where c.tipo = :tipo and c.valor = :email",
            Persona.class)
            .setParameter("tipo", TipoContacto.EMAIL)
            .setParameter("email", email)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<Persona> todos() {
        return createQuery("select p from Persona p", Persona.class).getResultList();
    }

    public void eliminar(Persona persona) {
        remove(persona);
    }

    public int cantidad() {
        return createQuery("select count(p) from Persona p", Long.class)
            .getSingleResult()
            .intValue();
    }
}
