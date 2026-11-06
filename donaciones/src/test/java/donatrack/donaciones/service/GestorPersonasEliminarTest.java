package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
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
    String documento = DatosDeTest.documentoUnico();
    String kcId = DatosDeTest.keycloakIdUnico();
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("ana")));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    Usuario usuario = Usuario.deDonante(kcId, donante);
    repoUsuarios.guardar(usuario);
    repoNotificaciones.guardar(Notificacion.paraDonante(
        donante, TipoNotificacion.CAMBIO_ESTADO, "hola", null));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar(documento);

    // Despues del gestor la transaccion esta commiteada y cerrada; nada de
    // flush aca: solo clear() y releer desde la base.
    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(documento).isPresent());
    assertFalse(repoPersonas.buscarPorDocumento(documento).isPresent());
    assertFalse(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
    assertEquals(List.of(kcId), proveedor.eliminados);
  }

  @Test
  void eliminarDonanteConDonacionesDa409YNoTocaKeycloak() {
    String documento = DatosDeTest.documentoUnico();
    PersonaHumana persona = new PersonaHumana("Luis", "G", 30, documento, Genero.MASCULINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("luis")));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    donacionDe(donante);

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    assertThrows(ConflictoDeNegocioException.class, () -> gestor.eliminar(documento));

    entityManager().clear();
    assertTrue(repoDonantes.buscarPorDocumento(documento).isPresent());
    assertTrue(proveedor.eliminados.isEmpty());
  }

  @Test
  void eliminarDonanteDeUnaPersonaJuridicaQueTambienEsBeneficiariaNoBorraLaPersona() {
    String cuit = DatosDeTest.cuitUnico();
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Dual"), TipoOrganizacion.ONG, "Rubro");
    juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("dual")));
    repoPersonas.guardar(juridica);
    Donante donante = new Donante(juridica);
    repoDonantes.guardar(donante);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    entityManager().flush();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar(cuit);

    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(cuit).isPresent(),
        "El Donante tiene que haberse borrado.");
    assertTrue(repoPersonas.buscarPorDocumento(cuit).isPresent(),
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
    String documento = DatosDeTest.documentoUnico();
    String kcId = DatosDeTest.keycloakIdUnico();
    PersonaHumana persona = new PersonaHumana("Maria", "X", 30, documento, Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("m")));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    repoUsuarios.guardar(Usuario.deDonante(kcId, donante));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso(true);
    GestorPersonas gestor = new GestorPersonas(
        repoDonantes, repoUsuarios, repoNotificaciones, repoDonaciones, repoEntidades, proveedor);

    gestor.eliminar(documento);

    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(documento).isPresent(),
        "Aunque Keycloak haya fallado, el donante local se borro.");
  }

  // === Helpers ===

  // Persistimos todo dentro de la transaccion que abre SimplePersistenceTest.
  // flush fuerza visibilidad antes de los chequeos de "tiene donaciones".
  private Donacion donacionDe(Donante donante) {
    Categoria cat = new Categoria(DatosDeTest.textoUnico("Cat"));
    entityManager().persist(cat);
    Subcategoria sub = new Subcategoria(DatosDeTest.textoUnico("Sub"), cat, Unidades.UNIDADES);
    entityManager().persist(sub);
    Bien bien = new Bien("Silla", sub, 1, CondicionBien.USADO);
    Donacion donacion = new Donacion(List.of(bien), donante, "demo");
    entityManager().persist(donacion);
    entityManager().flush();
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
