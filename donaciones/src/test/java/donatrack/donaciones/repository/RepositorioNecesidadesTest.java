package donatrack.donaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.necesidad.Necesidad;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.necesidad.NecesidadRecurrente;
import donatrack.donaciones.domain.necesidad.Periodo;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;

import java.util.List;

import org.junit.jupiter.api.Test;

class RepositorioNecesidadesTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioNecesidades repositorio = RepositorioNecesidades.getInstance();

  @Test
  void recurrenteYExtraordinariaConvivenEnTodas() {
    Subcategoria sub = subcategoriaAlimentos();
    Beneficiaria beneficiaria = crearBeneficiaria("30-11111111-1", "Comedor");

    repositorio.guardar(new NecesidadRecurrente("Arroz", 50, sub, beneficiaria, Periodo.MENSUAL));
    repositorio.guardar(new NecesidadExtraordinaria("Bancos", 30, sub, beneficiaria));

    entityManager().flush();
    entityManager().clear();

    List<Necesidad> todas = repositorio.todas();

    assertEquals(2, todas.size());
    assertTrue(todas.stream().anyMatch(n -> n instanceof NecesidadRecurrente));
    assertTrue(todas.stream().anyMatch(n -> n instanceof NecesidadExtraordinaria));
  }

  @Test
  void elPeriodoYElEsExtraordinariaVuelvenBien() {
    Subcategoria sub = subcategoriaAlimentos();
    Beneficiaria beneficiaria = crearBeneficiaria("30-22222222-2", "Comedor");

    repositorio.guardar(new NecesidadRecurrente("Arroz", 50, sub, beneficiaria, Periodo.MENSUAL));
    repositorio.guardar(new NecesidadExtraordinaria("Bancos", 30, sub, beneficiaria));

    entityManager().flush();
    entityManager().clear();

    List<Necesidad> todas = repositorio.todas();

    NecesidadRecurrente recurrente = todas.stream()
        .filter(NecesidadRecurrente.class::isInstance)
        .map(NecesidadRecurrente.class::cast)
        .findFirst()
        .orElseThrow();
    NecesidadExtraordinaria extraordinaria = todas.stream()
        .filter(NecesidadExtraordinaria.class::isInstance)
        .map(NecesidadExtraordinaria.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals(Periodo.MENSUAL, recurrente.getPeriodo());
    assertFalse(recurrente.esExtraordinaria());
    assertTrue(extraordinaria.esExtraordinaria());
  }

  @Test
  void unaNecesidadGuardadaVuelveConSuEntidad() {
    Subcategoria sub = subcategoriaAlimentos();
    Beneficiaria beneficiaria = crearBeneficiaria("30-33333333-3", "Escuela");

    Necesidad necesidad = new NecesidadRecurrente("Arroz", 50, sub, beneficiaria, Periodo.MENSUAL);
    repositorio.guardar(necesidad);

    entityManager().flush();
    entityManager().clear();

    Necesidad recuperada = repositorio.buscarPorId(necesidad.getId()).orElseThrow();

    assertEquals("30333333333", recuperada.getEntidad().getPersona().getCuit());
  }

  @Test
  void unaBeneficiariaConDosNecesidadesLasRecuperaAmbas() {
    Subcategoria sub = subcategoriaAlimentos();
    Beneficiaria beneficiaria = crearBeneficiaria("30-44444444-4", "Otra");

    repositorio.guardar(new NecesidadRecurrente("Arroz", 50, sub, beneficiaria, Periodo.MENSUAL));
    repositorio.guardar(new NecesidadExtraordinaria("Bancos", 30, sub, beneficiaria));

    entityManager().flush();
    entityManager().clear();

    Beneficiaria recuperada = repoEntidades.buscarPorCuit("30444444444").orElseThrow();

    assertEquals(2, recuperada.getNecesidades().size());
  }

  private Subcategoria subcategoriaAlimentos() {
    Categoria alimentos = new Categoria("Alimentos");
    entityManager().persist(alimentos);
    Subcategoria arroz = new Subcategoria("Arroz", alimentos, Unidades.KILOGRAMOS);
    entityManager().persist(arroz);
    return arroz;
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
