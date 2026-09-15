package donatrack.donaciones.repository;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaJuridica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioPersonas {

    private static RepositorioPersonas instancia;
    private final Map<String, Persona> porDocumento = new HashMap<>();
    private final Map<String, Persona> porEmail = new HashMap<>();

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
        if (porDocumento.containsKey(documento)) {
            throw new IllegalArgumentException(
                "Ya existe una persona registrada con documento " + documento
            );
        }
        porDocumento.put(documento, persona);
        emailDe(persona).ifPresent(email -> porEmail.put(email, persona));
    }

    public Optional<Persona> buscarPorDocumento(String documento) {
        return Optional.ofNullable(porDocumento.get(documento));
    }

    public Optional<PersonaJuridica> buscarJuridicaPorCuit(String cuit) {
        return buscarPorDocumento(cuit)
            .filter(PersonaJuridica.class::isInstance)
            .map(PersonaJuridica.class::cast);
    }

    public Optional<Persona> buscarPorEmail(String email) {
        return Optional.ofNullable(porEmail.get(email));
    }

    public List<Persona> todos() {
        return new ArrayList<>(porDocumento.values());
    }

    public void eliminar(Persona persona) {
        porDocumento.remove(persona.getDocumento());
        emailDe(persona).ifPresent(porEmail::remove);
    }

    public int cantidad() {
        return porDocumento.size();
    }

    private Optional<String> emailDe(Persona persona) {
        return persona.getContactos().stream()
            .filter(c -> c.getTipo() == TipoContacto.EMAIL)
            .map(MedioContacto::getValor)
            .findFirst();
    }
}
