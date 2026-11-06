package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorPersonasEliminarTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();
  private final RepositorioNotificaciones repoNotificaciones = new RepositorioNotificaciones();
  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();

  @Test
  void eliminarDonanteSinDonacionesBorraTodoYLlamaAKeycloak() {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, "11223344", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    Usuario usuario = Usuario.deDonante("kc-ana", donante);
    repoUsuarios.guardar(usuario);
    repoNotificaciones.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar("11223344");

    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento("11223344").isPresent());
    assertFalse(repoPersonas.buscarPorDocumento("11223344").isPresent());
    assertFalse(repoUsuarios.buscarPorKeycloakId("kc-ana").isPresent());
    assertEquals(List.of("kc-ana"), proveedor.eliminados);
  }

  @Test
  void eliminarDonanteConDonacionesDa409YNoTocaKeycloak() {
    PersonaHumana persona = new PersonaHumana("Luis", "G", 30, "22334455", Genero.MASCULINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "luis@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    donacionDe(donante);

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    assertThrows(ConflictoDeNegocioException.class, () -> gestor.eliminar("22334455"));

    entityManager().clear();
    assertTrue(repoDonantes.buscarPorDocumento("22334455").isPresent());
    assertTrue(proveedor.eliminados.isEmpty());
  }

  @Test
  void eliminarDonanteDeUnaPersonaJuridicaQueTambienEsBeneficiariaNoBorraLaPersona() {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-99887766-1", "Dual", TipoOrganizacion.ONG, "Rubro");
    juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "dual@mail.com"));
    repoPersonas.guardar(juridica);
    Donante donante = new Donante(juridica);
    repoDonantes.guardar(donante);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    entityManager().flush();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar(juridica.getCuit());

    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(juridica.getCuit()).isPresent(),
        "El Donante tiene que haberse borrado.");
    assertTrue(repoPersonas.buscarPorDocumento(juridica.getCuit()).isPresent(),
        "La persona sigue existiendo porque todavia es Beneficiaria.");
  }

  @Test
  void eliminarInexistenteDa404() {
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades,
        new ProveedorFalso());
    assertThrows(RecursoInexistenteException.class, () -> gestor.eliminar("no-existe"));
  }

  @Test
  void siKeycloakFallaElBorradoNoSeDeshace() {
    PersonaHumana persona = new PersonaHumana("Maria", "X", 30, "88776655", Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "m@mail.com"));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    repoUsuarios.guardar(Usuario.deDonante("kc-maria", donante));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso(true);
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar("88776655");

    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento("88776655").isPresent(),
        "Aunque Keycloak haya fallado, el donante local se borro.");
  }

  // === Helpers ===

  private Donacion donacionDe(Donante donante) {
    Categoria cat = new Categoria("Cat-" + System.nanoTime());
    entityManager().getTransaction().begin();
    entityManager().persist(cat);
    Subcategoria sub = new Subcategoria("Sub-" + System.nanoTime(), cat, Unidades.UNIDADES);
    entityManager().persist(sub);
    Bien bien = new Bien("Silla", sub, 1, CondicionBien.USADO);
    Donacion donacion = new Donacion(List.of(bien), donante, "demo");
    entityManager().persist(donacion);
    entityManager().getTransaction().commit();
    return donacion;
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> eliminados = new ArrayList<>();
    private final boolean falla;

    ProveedorFalso() { this(false); }
    ProveedorFalso(boolean falla) { this.falla = falla; }

    @Override public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public void eliminarUsuario(String idExterno) {
      if (falla) {
        throw new RuntimeException("Keycloak caido");
      }
      eliminados.add(idExterno);
    }
  }
}
