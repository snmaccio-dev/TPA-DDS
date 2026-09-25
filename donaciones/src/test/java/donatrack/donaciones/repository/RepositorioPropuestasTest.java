package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositorioPropuestasTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioPropuestas repositorio = new RepositorioPropuestas();

  @Test
  void unaPropuestaConTresCandidatasVuelveEnElMismoOrden() {
    Beneficiaria primera = crearBeneficiaria("30-11111111-1", "Primera");
    Beneficiaria segunda = crearBeneficiaria("30-22222222-2", "Segunda");
    Beneficiaria tercera = crearBeneficiaria("30-33333333-3", "Tercera");

    repositorio.guardar(new PropuestaAsignacion(
        42L, List.of(primera, segunda, tercera), LocalDateTime.now(), true
    ));

    entityManager().flush();
    entityManager().clear();

    PropuestaAsignacion recuperada = repositorio.buscarPorDonacion(42L).orElseThrow();
    List<Beneficiaria> candidatas = recuperada.getCandidatas();

    assertEquals(3, candidatas.size());
    assertEquals("Primera", candidatas.get(0).getPersona().getRazonSocial());
    assertEquals("Segunda", candidatas.get(1).getPersona().getRazonSocial());
    assertEquals("Tercera", candidatas.get(2).getPersona().getRazonSocial());
  }

  @Test
  void buscarPorDonacionEncuentraLaPropuestaGuardada() {
    Beneficiaria beneficiaria = crearBeneficiaria("30-44444444-4", "Escuela");

    repositorio.guardar(new PropuestaAsignacion(
        77L, List.of(beneficiaria), LocalDateTime.now(), false
    ));

    entityManager().flush();
    entityManager().clear();

    Optional<PropuestaAsignacion> recuperada = repositorio.buscarPorDonacion(77L);

    assertTrue(recuperada.isPresent());
    assertEquals(77L, recuperada.get().getDonacionId());
  }

  @Test
  void eliminarBorraLaPropuesta() {
    Beneficiaria beneficiaria = crearBeneficiaria("30-55555555-5", "Otra");

    repositorio.guardar(new PropuestaAsignacion(
        99L, List.of(beneficiaria), LocalDateTime.now(), false
    ));

    entityManager().flush();

    repositorio.eliminar(99L);

    entityManager().flush();
    entityManager().clear();

    assertTrue(repositorio.buscarPorDonacion(99L).isEmpty());
  }

  private Beneficiaria crearBeneficiaria(String cuit, String razonSocial) {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, razonSocial, TipoOrganizacion.INSTITUCION, "Rubro"
    );
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }
}
