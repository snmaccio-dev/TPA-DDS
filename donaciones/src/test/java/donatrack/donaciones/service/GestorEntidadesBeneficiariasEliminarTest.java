package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.necesidad.NecesidadExtraordinaria;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNecesidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorEntidadesBeneficiariasEliminarTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();
  private final RepositorioNotificaciones repoNotificaciones = new RepositorioNotificaciones();
  private final RepositorioNecesidades repoNecesidades = RepositorioNecesidades.getInstance();
  private final RepositorioPropuestas repoPropuestas = new RepositorioPropuestas();
  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();

  @Test
  void entidadConDonacionesAsignadasDa409() {
    Beneficiaria beneficiaria = beneficiariaPersistida("30-11111111-1");
    donacionAsignadaA(beneficiaria);

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorEntidadesBeneficiarias gestor = new GestorEntidadesBeneficiarias(
        repoUsuarios, repoNotificaciones, repoNecesidades, repoPropuestas,
        repoDonaciones, repoDonantes, proveedor);

    assertThrows(ConflictoDeNegocioException.class, () -> gestor.eliminar(beneficiaria.getId()));
  }

  @Test
  void entidadSinDonacionesBorraTodo() {
    Beneficiaria beneficiaria = beneficiariaPersistida("30-22222222-2");

    Subcategoria sub = subcategoriaPersistida("Bancos");
    entityManager().getTransaction().begin();
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria("Bancos", 10, sub, beneficiaria);
    entityManager().persist(necesidad);
    entityManager().getTransaction().commit();

    repoNotificaciones.guardar(Notificacion.paraBeneficiaria(
        beneficiaria, TipoNotificacion.DONACION_ASIGNADA, "msj", null));

    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorEntidadesBeneficiarias gestor = new GestorEntidadesBeneficiarias(
        repoUsuarios, repoNotificaciones, repoNecesidades, repoPropuestas,
        repoDonaciones, repoDonantes, proveedor);

    gestor.eliminar(beneficiaria.getId());

    entityManager().clear();
    assertFalse(repoEntidades.buscarPorId(beneficiaria.getId()).isPresent());
    Beneficiaria refrescada = entityManager().find(Beneficiaria.class, beneficiaria.getId());
    assertTrue(refrescada == null || refrescada.getNecesidades().isEmpty());
  }

  @Test
  void laBeneficiariaDesaparaceDeCandidatasSinBorrarLaPropuesta() {
    Beneficiaria aBorrar = beneficiariaPersistida("30-33333333-3");
    Beneficiaria otra = beneficiariaPersistida("30-44444444-4");

    // Armamos una propuesta con las dos como candidatas.
    entityManager().getTransaction().begin();
    PropuestaAsignacion propuesta = new PropuestaAsignacion(
        1L, List.of(aBorrar, otra), LocalDateTime.now(), true);
    entityManager().persist(propuesta);
    entityManager().getTransaction().commit();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorEntidadesBeneficiarias gestor = new GestorEntidadesBeneficiarias(
        repoUsuarios, repoNotificaciones, repoNecesidades, repoPropuestas,
        repoDonaciones, repoDonantes, proveedor);

    gestor.eliminar(aBorrar.getId());

    entityManager().clear();
    PropuestaAsignacion refrescada =
        repoPropuestas.buscarPorDonacion(1L).orElseThrow();
    assertEquals(1, refrescada.getCandidatas().size(),
        "La propuesta queda con la otra candidata.");
    assertEquals(otra.getId(), refrescada.getCandidatas().get(0).getId());
  }

  // === Helpers ===

  private Beneficiaria beneficiariaPersistida(String cuit) {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, "Benef " + cuit, TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    entityManager().flush();
    return beneficiaria;
  }

  private Subcategoria subcategoriaPersistida(String nombre) {
    Categoria cat = new Categoria("Cat-" + System.nanoTime());
    entityManager().getTransaction().begin();
    entityManager().persist(cat);
    Subcategoria sub = new Subcategoria(nombre + "-" + System.nanoTime(), cat, Unidades.UNIDADES);
    entityManager().persist(sub);
    entityManager().getTransaction().commit();
    return sub;
  }

  private void donacionAsignadaA(Beneficiaria beneficiaria) {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-00000099-9", "Donador", TipoOrganizacion.EMPRESA, "Rubro");
    repoPersonas.guardar(juridica);
    Donante donante = new Donante(juridica);
    repoDonantes.guardar(donante);
    Subcategoria sub = subcategoriaPersistida("Sillas");
    Bien bien = new Bien("Silla", sub, 1, CondicionBien.USADO);
    entityManager().getTransaction().begin();
    Donacion donacion = new Donacion(List.of(bien), donante, "demo");
    donacion.confirmarDestino(beneficiaria);
    entityManager().persist(donacion);
    entityManager().getTransaction().commit();
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> eliminados = new ArrayList<>();

    @Override public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public void eliminarUsuario(String idExterno) {
      eliminados.add(idExterno);
    }
  }
}
