package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

// NOTA: no implementa SimplePersistenceTest a proposito. El registro usa
// withTransaction de jpa-extras y queremos que esas transacciones se abran
// y commiteen de verdad (y hagan rollback real cuando corresponde) para
// poder verificar el efecto.
class DonantesServiceRegistroPublicoTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void registrarDonanteExitosoPersisteTodoYDevuelveElVinculo() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso(kcId);
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    String documento = DatosDeTest.documentoUnico();
    Persona persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);

    Donante donante = service.registrarPublico(persona, DatosDeTest.emailUnico("ana"), "unaSegura123");

    assertEquals(1, proveedor.creados.size());
    assertEquals(RolDeAcceso.DONANTE, proveedor.creados.get(0).rol);
    assertTrue(proveedor.creados.get(0).conContrasena);
    assertTrue(proveedor.eliminados.isEmpty());

    assertTrue(repoPersonas.buscarPorDocumento(documento).isPresent());
    assertTrue(repoDonantes.buscarPorDocumento(documento).isPresent());
    assertTrue(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
    assertEquals(donante.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).get().getDonante().getId());
  }

  @Test
  void documentoYaExistenteDevuelve409YNoTocaKeycloak() {
    ProveedorDeIdentidadFalso proveedor =
        new ProveedorDeIdentidadFalso(DatosDeTest.keycloakIdUnico());
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> {
      PersonaHumana existente = new PersonaHumana("Pre", "Existente", 40, documento, Genero.MASCULINO);
      repoPersonas.guardar(existente);
    });

    Persona nueva = new PersonaHumana("Nuevo", "Intento", 30, documento, Genero.FEMENINO);
    assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarPublico(nueva, DatosDeTest.emailUnico("nuevo"), "unaSegura123"));

    assertTrue(proveedor.creados.isEmpty(),
        "No se tiene que haber llamado a Keycloak si el documento ya existia.");
  }

  @Test
  void siKeycloakResponde409NoSePersisteNada() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso(
        () -> { throw new ConflictoDeNegocioException("Ya existe una cuenta con ese correo."); });
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    String documento = DatosDeTest.documentoUnico();
    Persona persona = new PersonaHumana("X", "Y", 30, documento, Genero.FEMENINO);
    assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarPublico(persona, DatosDeTest.emailUnico("x"), "unaSegura123"));

    assertFalse(repoPersonas.buscarPorDocumento(documento).isPresent());
  }

  @Test
  void siLaPersistenciaFallaDespuesDeCrearLaCuentaSeLlamaAEliminarUsuario() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso(kcId);
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(Usuario usuario) {
        throw new RuntimeException("DB caida simulada.");
      }
    };
    DonantesService service = nuevoService(proveedor, repoRoto);

    String documento = DatosDeTest.documentoUnico();
    Persona persona = new PersonaHumana("Z", "W", 30, documento, Genero.FEMENINO);
    assertThrows(RuntimeException.class, () ->
        service.registrarPublico(persona, DatosDeTest.emailUnico("z"), "unaSegura123"));

    assertEquals(List.of(kcId), proveedor.eliminados,
        "Si la persistencia fallo despues de crear la cuenta, hay que borrarla en Keycloak.");
  }

  @Test
  void contrasenaCortaDevuelve400YNoLlamaAKeycloak() {
    ProveedorDeIdentidadFalso proveedor =
        new ProveedorDeIdentidadFalso(DatosDeTest.keycloakIdUnico());
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    String documento = DatosDeTest.documentoUnico();
    Persona persona = new PersonaHumana("Q", "R", 30, documento, Genero.FEMENINO);
    assertThrows(DatosInvalidosException.class, () ->
        service.registrarPublico(persona, DatosDeTest.emailUnico("q"), "1234"));

    assertTrue(proveedor.creados.isEmpty());
  }

  private DonantesService nuevoService(ProveedorDeIdentidad proveedor, RepositorioUsuarios usuarios) {
    return new DonantesService(
        repoDonantes,
        repoPersonas,
        new RepositorioNotificaciones(),
        RepositorioDonaciones.getInstance(),
        RepositorioEntidades.getInstance(),
        mock(NotificacionesService.class),
        new UsuariosService(proveedor, usuarios));
  }

  // === ProveedorDeIdentidad falso que registra las llamadas ===

  private static class ProveedorDeIdentidadFalso implements ProveedorDeIdentidad {
    private final String idFijo;
    private final CreadorFallando creadorFallando;
    final List<LlamadaCreacion> creados = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();

    ProveedorDeIdentidadFalso(String idFijo) {
      this.idFijo = idFijo;
      this.creadorFallando = null;
    }

    ProveedorDeIdentidadFalso(CreadorFallando creadorFallando) {
      this.idFijo = null;
      this.creadorFallando = creadorFallando;
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      if (creadorFallando != null) {
        creadorFallando.ejecutar();
      }
      creados.add(new LlamadaCreacion(email, rol, contrasena != null));
      return idFijo;
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      if (creadorFallando != null) {
        creadorFallando.ejecutar();
      }
      creados.add(new LlamadaCreacion(email, rol, false));
      return idFijo;
    }

    @Override
    public java.util.Optional<String> buscarIdPorEmail(String email) {
      return java.util.Optional.empty();
    }

    @Override
    public void eliminarUsuario(String idExterno) {
      eliminados.add(idExterno);
    }
  }

  @FunctionalInterface
  private interface CreadorFallando {
    void ejecutar();
  }

  private record LlamadaCreacion(String email, RolDeAcceso rol, boolean conContrasena) {}
}
