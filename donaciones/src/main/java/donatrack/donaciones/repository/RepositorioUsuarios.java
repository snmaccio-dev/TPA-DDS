package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.usuario.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RepositorioUsuarios implements WithSimplePersistenceUnit {

    public void guardar(Usuario usuario) {
        persist(usuario);
    }

    public void eliminar(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        remove(usuario);
    }

    public Optional<Usuario> buscarPorKeycloakId(String keycloakId) {
        return Optional.ofNullable(find(Usuario.class, keycloakId));
    }

    public Optional<Usuario> deDonante(Donante donante) {
        if (donante == null || donante.getId() == null) {
            return Optional.empty();
        }
        return createQuery(
            "select u from Usuario u where u.donante = :donante",
            Usuario.class)
            .setParameter("donante", donante)
            .getResultList()
            .stream()
            .findFirst();
    }

    ////////////////////////////////////////////////////////////////

    public List<Usuario> deDonantes(Collection<Donante> donantes) {
        if (donantes == null || donantes.isEmpty()) {
            return List.of();
        }

        return createQuery(
            "select u from Usuario u where u.donante in :donantes",
            Usuario.class)
            .setParameter("donantes", donantes)
            .getResultList();
    }

    public Optional<Usuario> deBeneficiaria(Beneficiaria beneficiaria) {
        if (beneficiaria == null || beneficiaria.getId() == null) {
            return Optional.empty();
        }
        return createQuery(
            "select u from Usuario u where u.beneficiaria = :beneficiaria",
            Usuario.class)
            .setParameter("beneficiaria", beneficiaria)
            .getResultList()
            .stream()
            .findFirst();
    }

    public boolean existeParaDonante(Donante donante) {
        return deDonante(donante).isPresent();
    }

    public boolean existeParaBeneficiaria(Beneficiaria beneficiaria) {
        return deBeneficiaria(beneficiaria).isPresent();
    }
}
