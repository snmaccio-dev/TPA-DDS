package donatrack.repositorio;

import donatrack.model.entidad.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Singleton — unica instancia de almacen de entidades beneficiarias en memoria
public class RepositorioEntidades {

    private static RepositorioEntidades instancia;
    private final Map<String, EntidadBeneficiaria> porRazonSocial = new HashMap<>();

    private RepositorioEntidades() {}

    public static RepositorioEntidades getInstance() {
        if (instancia == null) {
            instancia = new RepositorioEntidades();
        }
        return instancia;
    }

    public void guardar(EntidadBeneficiaria entidad) {
        porRazonSocial.put(entidad.getRazonSocial(), entidad);
    }

    public Optional<EntidadBeneficiaria> buscarPorRazonSocial(String razonSocial) {
        return Optional.ofNullable(porRazonSocial.get(razonSocial));
    }

    public List<EntidadBeneficiaria> todas() {
        return new ArrayList<>(porRazonSocial.values());
    }
}
