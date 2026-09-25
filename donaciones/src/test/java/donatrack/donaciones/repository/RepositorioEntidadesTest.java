package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositorioEntidadesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repositorio = RepositorioEntidades.getInstance();

  @Test
  void unaBeneficiariaGuardadaSeRecuperaPorCuit() {
    PersonaJuridica escuela = new PersonaJuridica(
        "30-77777777-1", "Escuela Rural N10", TipoOrganizacion.INSTITUCION, "Educacion"
    );
    escuela.setDireccion("Ruta 3 km 42");
    repoPersonas.guardar(escuela);
    Beneficiaria beneficiaria = new Beneficiaria(escuela);
    repositorio.guardar(beneficiaria);

    entityManager().flush();
    entityManager().clear();

    Optional<Beneficiaria> recuperada = repositorio.buscarPorCuit("30777777771");

    assertTrue(recuperada.isPresent());
    assertEquals("Escuela Rural N10", recuperada.get().getPersona().getRazonSocial());
  }

  @Test
  void losRepresentantesVuelvenDeLaBaseConSuEmail() {
    PersonaJuridica arcos = new PersonaJuridica(
        "30-12345678-1", "Arcos S.A.", TipoOrganizacion.EMPRESA, "Construccion"
    );
    arcos.agregarRepresentante(new Representante("Ana", "Perez", "ana@mail.com"));
    arcos.agregarRepresentante(new Representante("Luis", "Garcia", "luis@mail.com"));
    repoPersonas.guardar(arcos);
    Beneficiaria beneficiaria = new Beneficiaria(arcos);
    repositorio.guardar(beneficiaria);

    entityManager().flush();
    entityManager().clear();

    Beneficiaria recuperada = repositorio.buscarPorCuit("30123456781").orElseThrow();
    List<Representante> reps = recuperada.getPersona().getRepresentantes();

    assertEquals(2, reps.size());
    assertTrue(reps.stream().anyMatch(r -> "ana@mail.com".equals(r.getEmail())));
    assertTrue(reps.stream().anyMatch(r -> "luis@mail.com".equals(r.getEmail())));
  }

  @Test
  void buscarPorIdFuncionaConElIdAsignadoPorLaBase() {
    PersonaJuridica escuela = new PersonaJuridica(
        "30-88888888-1", "Escuela", TipoOrganizacion.INSTITUCION, "Educacion"
    );
    repoPersonas.guardar(escuela);
    Beneficiaria beneficiaria = new Beneficiaria(escuela);
    repositorio.guardar(beneficiaria);

    entityManager().flush();

    Long id = beneficiaria.getId();
    assertNotNull(id);

    entityManager().clear();

    Optional<Beneficiaria> recuperada = repositorio.buscarPorId(id);

    assertTrue(recuperada.isPresent());
    assertEquals("Escuela", recuperada.get().getPersona().getRazonSocial());
  }
}
