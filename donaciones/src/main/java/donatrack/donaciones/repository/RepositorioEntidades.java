package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Beneficiaria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Optional;

public class RepositorioEntidades implements WithSimplePersistenceUnit {

    private static RepositorioEntidades instancia;

    private RepositorioEntidades() {
    }

    public static RepositorioEntidades getInstance() {
        if (instancia == null) {
            instancia = new RepositorioEntidades();
        }
        return instancia;
    }

    public void guardar(Beneficiaria beneficiaria) {
        persist(beneficiaria);
    }

    public Optional<Beneficiaria> buscarPorId(long id) {
        return Optional.ofNullable(find(Beneficiaria.class, id));
    }

    public Optional<Beneficiaria> buscarPorCuit(String cuit) {
        return createQuery(
            "select b from Beneficiaria b, PersonaJuridica p "
                + "where b.persona = p and p.cuit = :cuit",
            Beneficiaria.class)
            .setParameter("cuit", cuit)
            .getResultList()
            .stream()
            .findFirst();
    }

    public List<Beneficiaria> todas() {
        return createQuery("select b from Beneficiaria b", Beneficiaria.class).getResultList();
    }

    public void eliminar(long id) {
        buscarPorId(id).ifPresent(this::remove);
    }
}
