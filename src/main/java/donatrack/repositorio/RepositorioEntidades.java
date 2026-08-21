package donatrack.repositorio;

import donatrack.model.persona.Beneficiaria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Singleton — unica instancia de almacen de beneficiarias en memoria
public class RepositorioEntidades {

    private static RepositorioEntidades instancia;

    private final Map<Long, Beneficiaria> beneficiarias = new HashMap<>();

    private RepositorioEntidades() {
    }

    public static RepositorioEntidades getInstance() {
        if (instancia == null) {
            instancia = new RepositorioEntidades();
        }
        return instancia;
    }

    public void guardar(Beneficiaria beneficiaria) {
        beneficiarias.put(beneficiaria.getId(), beneficiaria);
    }

    public Optional<Beneficiaria> buscarPorId(long id) {
        return Optional.ofNullable(beneficiarias.get(id));
    }

    public List<Beneficiaria> todas() {
        return new ArrayList<>(beneficiarias.values());
    }

    public void eliminar(long id) {
        beneficiarias.remove(id);
    }
}
