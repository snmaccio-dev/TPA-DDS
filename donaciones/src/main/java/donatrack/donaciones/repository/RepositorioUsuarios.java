package donatrack.donaciones.repository;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.usuario.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.Optional;

public class RepositorioUsuarios implements WithSimplePersistenceUnit {

    public void guardar(Usuario usuario) {
        persist(usuario);
    }

    public Optional<Usuario> buscarPorKeycloakId(String keycloakId) {
        return Optional.ofNullable(find(Usuario.class, keycloakId));
    }

    public boolean existeParaDonante(Donante donante) {
        if (donante == null || donante.getId() == null) {
            return false;
        }
        return createQuery(
            "select count(u) from Usuario u where u.donante = :donante",
            Long.class)
            .setParameter("donante", donante)
            .getSingleResult() > 0;
    }

    public boolean existeParaBeneficiaria(Beneficiaria beneficiaria) {
        if (beneficiaria == null || beneficiaria.getId() == null) {
            return false;
        }
        return createQuery(
            "select count(u) from Usuario u where u.beneficiaria = :beneficiaria",
            Long.class)
            .setParameter("beneficiaria", beneficiaria)
            .getSingleResult() > 0;
    }
}