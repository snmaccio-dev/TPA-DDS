package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
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
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NOTA: no implementa SimplePersistenceTest a proposito, por el mismo motivo
// que DonantesServiceRegistroPublicoTest.
class EntidadesBeneficiariasServiceRegistroPublicoTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void registrarBeneficiariaExitosoPersisteTodo() {
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso(kcId);
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    String cuit = DatosDeTest.cuitUnico();
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Comedor"), TipoOrganizacion.ONG, "Comedor");

    Beneficiaria beneficiaria = service.registrarPublica(
        juridica, datos(), DatosDeTest.emailUnico("benef"), "unaSegura123");

    assertSame(RolDeAcceso.BENEFICIARIA, proveedor.creados.get(0).rol);
    assertTrue(proveedor.creados.get(0).conContrasena);
    assertTrue(repoEntidades.buscarPorCuit(juridica.getCuit()).isPresent());
    assertTrue(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
    assertEquals(beneficiaria.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).get().getBeneficiaria().getId());
  }

  @Test
  void cuitYaExistenteDevuelve409YNoTocaKeycloak() {
    ProveedorDeIdentidadFalso proveedor =
        new ProveedorDeIdentidadFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> repoPersonas.guardar(new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Previa"), TipoOrganizacion.ONG, "Rubro")));

    PersonaJuridica nueva = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Nueva"), TipoOrganizacion.ONG, "Rubro");
    assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarPublica(nueva, datos(), DatosDeTest.emailUnico("dup"), "unaSegura123"));

    assertTrue(proveedor.creados.isEmpty());
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
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoRoto);

    String cuit = DatosDeTest.cuitUnico();
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Rota"), TipoOrganizacion.ONG, "Rubro");
    assertThrows(RuntimeException.class, () ->
        service.registrarPublica(juridica, datos(), DatosDeTest.emailUnico("rota"), "unaSegura123"));

    assertEquals(List.of(kcId), proveedor.eliminados);
  }

  @Test
  void contrasenaCortaDevuelve400YNoLlamaAKeycloak() {
    ProveedorDeIdentidadFalso proveedor =
        new ProveedorDeIdentidadFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    PersonaJuridica juridica = new PersonaJuridica(
        DatosDeTest.cuitUnico(), DatosDeTest.textoUnico("Corta"), TipoOrganizacion.ONG, "Rubro");
    assertThrows(DatosInvalidosException.class, () ->
        service.registrarPublica(juridica, datos(), DatosDeTest.emailUnico("corta"), "1234"));

    assertTrue(proveedor.creados.isEmpty());
  }

  private DatosEntidad datos() {
    return new DatosEntidad(
        "Calle Falsa 123",
        "+54 11 0000-0000",
        List.of(new Representante("Rep", "Uno", DatosDeTest.emailUnico("rep1")))
    );
  }

  private EntidadesBeneficiariasService nuevoService(ProveedorDeIdentidad proveedor,
                                                     RepositorioUsuarios usuarios) {
    return new EntidadesBeneficiariasService(
        repoEntidades,
        repoPersonas,
        new RepositorioNotificaciones(),
        RepositorioNecesidades.getInstance(),
        new RepositorioPropuestas(),
        RepositorioDonaciones.getInstance(),
        new RepositorioDonantes(),
        new UsuariosService(proveedor, usuarios));
  }

  private static class ProveedorDeIdentidadFalso implements ProveedorDeIdentidad {
    private final String idFijo;
    final List<LlamadaCreacion> creados = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();

    ProveedorDeIdentidadFalso(String idFijo) {
      this.idFijo = idFijo;
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      creados.add(new LlamadaCreacion(email, rol, contrasena != null));
      return idFijo;
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
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

  private record LlamadaCreacion(String email, RolDeAcceso rol, boolean conContrasena) {}
}
