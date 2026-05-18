package donatrack.repositorio;

import donatrack.model.persona.Persona;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Singleton — unica instancia de almacen de personas en memoria
public class RepositorioPersonas {

    private static RepositorioPersonas instancia;
    private final Map<String, Persona> porEmail = new HashMap<>();

    private RepositorioPersonas() {}

    public static RepositorioPersonas getInstance() {
        if (instancia == null) {
            instancia = new RepositorioPersonas();
        }
        return instancia;
    }

    public void guardar(String email, Persona persona) {
        porEmail.put(email, persona);
    }

    public Optional<Persona> buscarPorEmail(String email) {
        return Optional.ofNullable(porEmail.get(email));
    }

    public boolean existe(String email) {
        return porEmail.containsKey(email);
    }

    public List<Persona> todos() {
        return new ArrayList<>(porEmail.values());
    }

    public int cantidad() {
        return porEmail.size();
    }
}
