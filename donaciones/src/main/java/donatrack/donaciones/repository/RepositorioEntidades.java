package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Singleton — unica instancia de almacen de beneficiarias en memoria
public class RepositorioEntidades {

    private static RepositorioEntidades instancia;

    private final Map<Long, Beneficiaria> beneficiarias = new HashMap<>();
    private final Map<String, Beneficiaria> porCuit = new HashMap<>();

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
        porCuit.put(beneficiaria.getPersona().getCuit(), beneficiaria);
    }

    public Optional<Beneficiaria> buscarPorId(long id) {
        return Optional.ofNullable(beneficiarias.get(id));
    }

    public Optional<Beneficiaria> buscarPorCuit(String cuit) {
        return Optional.ofNullable(porCuit.get(cuit));
    }

    public List<Beneficiaria> todas() {
        return new ArrayList<>(beneficiarias.values());
    }

    public void eliminar(long id) {
        Beneficiaria beneficiaria = beneficiarias.remove(id);
        if (beneficiaria != null) {
            porCuit.remove(beneficiaria.getPersona().getCuit());
        }
    }
}
