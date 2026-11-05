package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RepositorioDonantes implements WithSimplePersistenceUnit {

    public void guardar(Donante donante) {
        persist(donante);
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
            .setParameter("documento", documento)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<Donante> todos() {
        return createQuery("select d from Donante d", Donante.class).getResultList();
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
}
