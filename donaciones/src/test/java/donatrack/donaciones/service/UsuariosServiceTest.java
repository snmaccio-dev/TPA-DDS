package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// No implementa SimplePersistenceTest: UsuariosService usa withTransaction y
// queremos que esas transacciones se abran y commiteen de verdad.
class UsuariosServiceTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  // === crearEnProveedor ===

  @Test
  void crearEnProveedorSinContrasenaUsaElAltaSinContrasena() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);
    String email = DatosDeTest.emailUnico("sin-pass");

    String id = service.crearEnProveedor(email, null, RolDeAcceso.DONANTE);

    assertEquals(proveedor.idFijo, id);
    assertEquals(1, proveedor.creados.size());
    assertEquals(new Creacion(email, null, RolDeAcceso.DONANTE, false), proveedor.creados.get(0));
  }

  @Test
  void crearEnProveedorConContrasenaUsaElAltaConContrasena() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);
    String email = DatosDeTest.emailUnico("con-pass");

    service.crearEnProveedor(email, "unaSegura123", RolDeAcceso.BENEFICIARIA);

    assertEquals(
        List.of(new Creacion(email, "unaSegura123", RolDeAcceso.BENEFICIARIA, true)),
        proveedor.creados);
  }

  // === vincular / desvincular ===

  @Test
  void vincularUnDonanteGuardaElUsuario() {
    String kcId = DatosDeTest.keycloakIdUnico();
    UsuariosService service = new UsuariosService(new ProveedorFalso(kcId), repoUsuarios);

    Donante[] donante = new Donante[1];
    withTransaction(() -> {
      donante[0] = donantePersistido();
      service.vincular(kcId, donante[0]);
    });

    entityManager().clear();
    Usuario usuario = repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow();
    assertEquals(donante[0].getId(), usuario.getDonante().getId());
  }

  @Test
  void vincularUnaBeneficiariaGuardaElUsuario() {
    String kcId = DatosDeTest.keycloakIdUnico();
    UsuariosService service = new UsuariosService(new ProveedorFalso(kcId), repoUsuarios);

    Beneficiaria[] beneficiaria = new Beneficiaria[1];
    withTransaction(() -> {
      beneficiaria[0] = beneficiariaPersistida();
      service.vincular(kcId, beneficiaria[0]);
    });

    entityManager().clear();
    Usuario usuario = repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow();
    assertEquals(beneficiaria[0].getId(), usuario.getBeneficiaria().getId());
  }

  @Test
  void desvincularUnDonanteBorraElUsuarioYDevuelveSuId() {
    String kcId = DatosDeTest.keycloakIdUnico();
    UsuariosService service = new UsuariosService(new ProveedorFalso(kcId), repoUsuarios);
    Donante[] donante = new Donante[1];
    withTransaction(() -> {
      donante[0] = donantePersistido();
      repoUsuarios.guardar(Usuario.deDonante(kcId, donante[0]));
    });

    List<Optional<String>> resultado = new ArrayList<>();
    withTransaction(() -> resultado.add(service.desvincular(donante[0])));

    assertEquals(Optional.of(kcId), resultado.get(0));
    entityManager().clear();
    assertFalse(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
  }

  @Test
  void desvincularUnaBeneficiariaBorraElUsuarioYDevuelveSuId() {
    String kcId = DatosDeTest.keycloakIdUnico();
    UsuariosService service = new UsuariosService(new ProveedorFalso(kcId), repoUsuarios);
    Beneficiaria[] beneficiaria = new Beneficiaria[1];
    withTransaction(() -> {
      beneficiaria[0] = beneficiariaPersistida();
      repoUsuarios.guardar(Usuario.deBeneficiaria(kcId, beneficiaria[0]));
    });

    List<Optional<String>> resultado = new ArrayList<>();
    withTransaction(() -> resultado.add(service.desvincular(beneficiaria[0])));

    assertEquals(Optional.of(kcId), resultado.get(0));
    entityManager().clear();
    assertFalse(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
  }

  @Test
  void desvincularUnPerfilSinCuentaDevuelveVacio() {
    UsuariosService service = new UsuariosService(new ProveedorFalso(null), repoUsuarios);
    Donante[] donante = new Donante[1];
    withTransaction(() -> donante[0] = donantePersistido());

    List<Optional<String>> resultado = new ArrayList<>();
    withTransaction(() -> resultado.add(service.desvincular(donante[0])));

    assertEquals(Optional.empty(), resultado.get(0));
  }

  // === compensar / eliminarEnProveedor ===

  @Test
  void compensarBorraLaCuentaEnElProveedor() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    service.compensar(kcId);

    assertEquals(List.of(kcId), proveedor.eliminados);
  }

  @Test
  void compensarNoLanzaSiFallaKeycloak() {
    ProveedorFalso proveedor = ProveedorFalso.queFallaAlEliminar();
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    assertDoesNotThrow(() -> service.compensar(DatosDeTest.keycloakIdUnico()));
  }

  @Test
  void eliminarEnProveedorBorraLaCuenta() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    service.eliminarEnProveedor(kcId);

    assertEquals(List.of(kcId), proveedor.eliminados);
  }

  @Test
  void eliminarEnProveedorNoLanzaSiFallaKeycloak() {
    ProveedorFalso proveedor = ProveedorFalso.queFallaAlEliminar();
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    assertDoesNotThrow(() -> service.eliminarEnProveedor(DatosDeTest.keycloakIdUnico()));
  }

  // === crearAdministrador ===

  @Test
  void crearAdministradorUsaLaContrasenaYElRolAdministrador() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);
    String email = DatosDeTest.emailUnico("admin");

    String id = service.crearAdministrador(email, "unaLarga123");

    assertEquals(kcId, id);
    assertEquals(
        List.of(new Creacion(email, "unaLarga123", RolDeAcceso.ADMINISTRADOR, true)),
        proveedor.creados);
  }

  @Test
  void crearAdministradorSinEmailDa400YNoLlamaAKeycloak() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    assertThrows(DatosInvalidosException.class, () ->
        service.crearAdministrador(" ", "unaLarga123"));
    assertTrue(proveedor.creados.isEmpty());
  }

  @Test
  void crearAdministradorConContrasenaCortaDa400YNoLlamaAKeycloak() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    UsuariosService service = new UsuariosService(proveedor, repoUsuarios);

    assertThrows(DatosInvalidosException.class, () ->
        service.crearAdministrador(DatosDeTest.emailUnico("admin"), "1234"));
    assertTrue(proveedor.creados.isEmpty());
  }

  // === Helpers ===

  // Espera una transaccion abierta.
  private Donante donantePersistido() {
    PersonaHumana persona = new PersonaHumana(
        "Ana", "Test", 30, DatosDeTest.documentoUnico(), Genero.FEMENINO);
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    return donante;
  }

  // Espera una transaccion abierta.
  private Beneficiaria beneficiariaPersistida() {
    PersonaJuridica juridica = new PersonaJuridica(
        DatosDeTest.cuitUnico(), DatosDeTest.textoUnico("Benef"), TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    return beneficiaria;
  }

  private record Creacion(String email, String contrasena, RolDeAcceso rol, boolean conContrasena) {}

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<Creacion> creados = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();
    final String idFijo;
    private final boolean fallaAlEliminar;

    ProveedorFalso(String idFijo) {
      this(idFijo, false);
    }

    private ProveedorFalso(String idFijo, boolean fallaAlEliminar) {
      this.idFijo = idFijo;
      this.fallaAlEliminar = fallaAlEliminar;
    }

    static ProveedorFalso queFallaAlEliminar() {
      return new ProveedorFalso(DatosDeTest.keycloakIdUnico(), true);
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      creados.add(new Creacion(email, contrasena, rol, true));
      return idFijo;
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      creados.add(new Creacion(email, null, rol, false));
      return idFijo;
    }

    @Override
    public java.util.Optional<String> buscarIdPorEmail(String email) {
      return java.util.Optional.empty();
    }

    @Override
    public void eliminarUsuario(String idExterno) {
      if (fallaAlEliminar) {
        throw new RuntimeException("Keycloak caido");
      }
      eliminados.add(idExterno);
    }
  }
}
