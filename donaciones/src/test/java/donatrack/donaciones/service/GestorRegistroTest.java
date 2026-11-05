package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NOTA: no implementa SimplePersistenceTest a proposito. GestorRegistro usa
// withTransaction de jpa-extras y queremos que esas transacciones se abran
// y commiteen de verdad (y hagan rollback real cuando corresponde) para
// poder verificar el efecto. Cada test usa datos unicos para no colisionar
// con datos de otros tests sobre la misma HSQLDB en memoria.
public class GestorRegistroTest implements WithSimplePersistenceUnit {

  private static final AtomicInteger CONTADOR = new AtomicInteger(0);

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void registrarDonanteExitosoPersisteTodoYDevuelveElVinculo() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-donante-1");
    GestorRegistro gestor = nuevoGestor(proveedor);

    String documento = unico("D");
    Persona persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);

    Donante donante = gestor.registrarDonante(persona, "ana+" + documento + "@mail.com", "unaSegura123");

    assertEquals(1, proveedor.creados.size());
    assertEquals(RolDeAcceso.DONANTE, proveedor.creados.get(0).rol);
    assertTrue(proveedor.eliminados.isEmpty());

    assertTrue(repoPersonas.buscarPorDocumento(documento).isPresent());
    assertTrue(repoDonantes.buscarPorDocumento(documento).isPresent());
    assertTrue(repoUsuarios.buscarPorKeycloakId("kc-donante-1").isPresent());
    assertEquals(donante.getId(),
        repoUsuarios.buscarPorKeycloakId("kc-donante-1").get().getDonante().getId());
  }

  @Test
  void documentoYaExistenteDevuelve409YNoTocaKeycloak() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-se-usa");
    GestorRegistro gestor = nuevoGestor(proveedor);

    String documento = unico("DUP");
    // Precondicion: ya hay una persona con ese documento.
    withTransaction(() -> {
      PersonaHumana existente = new PersonaHumana("Pre", "Existente", 40, documento, Genero.MASCULINO);
      repoPersonas.guardar(existente);
    });

    Persona nueva = new PersonaHumana("Nuevo", "Intento", 30, documento, Genero.FEMENINO);
    assertThrows(ConflictoDeNegocioException.class, () ->
        gestor.registrarDonante(nueva, "nuevo@mail.com", "unaSegura123"));

    assertTrue(proveedor.creados.isEmpty(),
        "No se tiene que haber llamado a Keycloak si el documento ya existia.");
  }

  @Test
  void siKeycloakResponde409NoSePersisteNada() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso(
        () -> { throw new ConflictoDeNegocioException("Ya existe una cuenta con ese correo."); });
    GestorRegistro gestor = nuevoGestor(proveedor);

    String documento = unico("KC409");
    Persona persona = new PersonaHumana("X", "Y", 30, documento, Genero.FEMENINO);
    assertThrows(ConflictoDeNegocioException.class, () ->
        gestor.registrarDonante(persona, "x@mail.com", "unaSegura123"));

    assertFalse(repoPersonas.buscarPorDocumento(documento).isPresent());
  }

  @Test
  void siLaPersistenciaFallaDespuesDeCrearLaCuentaSeLlamaAEliminarUsuario() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-rollback");
    // Rompemos la persistencia haciendo que el repositorio de usuarios falle.
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(donatrack.donaciones.domain.usuario.Usuario usuario) {
        throw new RuntimeException("DB caida simulada.");
      }
    };
    GestorRegistro gestor = new GestorRegistro(
        proveedor, repoPersonas, repoDonantes, repoEntidades, repoRoto);

    String documento = unico("ROLL");
    Persona persona = new PersonaHumana("Z", "W", 30, documento, Genero.FEMENINO);
    assertThrows(RuntimeException.class, () ->
        gestor.registrarDonante(persona, "z@mail.com", "unaSegura123"));

    assertEquals(List.of("kc-rollback"), proveedor.eliminados,
        "Si la persistencia fallo despues de crear la cuenta, hay que borrarla en Keycloak.");
  }

  @Test
  void registrarBeneficiariaExitosoPersisteTodo() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-benef-1");
    GestorRegistro gestor = nuevoGestor(proveedor);

    String cuit = "30-" + String.format("%08d", Math.abs(unico("B").hashCode() % 100_000_000)) + "-1";
    PersonaJuridica juridica = new PersonaJuridica(cuit, "Comedor Test", TipoOrganizacion.ONG, "Comedor");
    DatosEntidad datos = new DatosEntidad(
        "Calle Falsa 123",
        "+54 11 0000-0000",
        List.of(new Representante("Rep", "Uno", "rep1@mail.com"))
    );

    Beneficiaria beneficiaria =
        gestor.registrarBeneficiaria(juridica, datos, "benef@mail.com", "unaSegura123");

    assertSame(RolDeAcceso.BENEFICIARIA, proveedor.creados.get(0).rol);
    assertTrue(repoEntidades.buscarPorCuit(juridica.getCuit()).isPresent());
    assertTrue(repoUsuarios.buscarPorKeycloakId("kc-benef-1").isPresent());
    assertEquals(beneficiaria.getId(),
        repoUsuarios.buscarPorKeycloakId("kc-benef-1").get().getBeneficiaria().getId());
  }

  @Test
  void contrasenaCortaDevuelve400YNoLlamaAKeycloak() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-llega");
    GestorRegistro gestor = nuevoGestor(proveedor);

    String documento = unico("SHORT");
    Persona persona = new PersonaHumana("Q", "R", 30, documento, Genero.FEMENINO);
    assertThrows(DatosInvalidosException.class, () ->
        gestor.registrarDonante(persona, "q@mail.com", "1234"));

    assertTrue(proveedor.creados.isEmpty());
  }

  private GestorRegistro nuevoGestor(ProveedorDeIdentidad proveedor) {
    return new GestorRegistro(proveedor, repoPersonas, repoDonantes, repoEntidades, repoUsuarios);
  }

  private static String unico(String prefijo) {
    // Documento de 8 digitos. Garantizamos unicidad por tests sobre la misma DB.
    int n = CONTADOR.incrementAndGet();
    return String.format("%s%07d", prefijo, n).substring(0, 8);
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
      creados.add(new LlamadaCreacion(email, rol, Optional.ofNullable(contrasena).isPresent()));
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