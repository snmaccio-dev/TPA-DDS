package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.contacto.TipoContacto;
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
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DonantesServiceAltaTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void conPersonaNuevaCreaPersonaDonanteYVinculo() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("nueva");
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    service.registrarConPersonaNueva(
        new PersonaHumana("Nora", "Diaz", 35, documento, Genero.FEMENINO), email);

    assertEquals(List.of(email), proveedor.emailsCreados);
    entityManager().clear();
    Donante creado = repoDonantes.buscarPorDocumento(documento).orElseThrow();
    assertEquals(creado.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow().getDonante().getId());
    assertTrue(creado.getPersona().tieneContacto(TipoContacto.EMAIL, email));
  }

  @Test
  void conPersonaNuevaCuyoDocumentoYaExisteDa409SinLlamarAKeycloak() {
    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> personaPersistida(documento));
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarConPersonaNueva(
            new PersonaHumana("Otra", "Persona", 40, documento, Genero.MASCULINO),
            DatosDeTest.emailUnico("dup")));

    assertTrue(proveedor.emailsCreados.isEmpty());
    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(documento).isPresent());
  }

  @Test
  void conPersonaExistenteQueNoEsDonanteCreaElRolYLaCuenta() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> beneficiariaPersistida(cuit));
    String email = DatosDeTest.emailUnico("juridica");
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    service.registrarConPersonaExistente(cuit, email);

    assertEquals(List.of(email), proveedor.emailsCreados);
    entityManager().clear();
    Donante creado = repoDonantes.buscarPorDocumento(cuit).orElseThrow();
    assertEquals(creado.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow().getDonante().getId());
    assertTrue(creado.getPersona().tieneContacto(TipoContacto.EMAIL, email));
  }

  @Test
  void conPersonaExistenteQueYaEsDonanteDa409SinLlamarAKeycloak() {
    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> {
      Donante donante = new Donante(personaPersistida(documento));
      repoDonantes.guardar(donante);
      repoUsuarios.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));
    });
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    ConflictoDeNegocioException error = assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarConPersonaExistente(documento, DatosDeTest.emailUnico("ya-donante")));

    assertEquals("La persona ya es donante.", error.getMessage());
    assertTrue(proveedor.emailsCreados.isEmpty());
  }

  @Test
  void conUnDocumentoInexistenteDa404() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    DonantesService service = nuevoService(proveedor, repoUsuarios);

    assertThrows(RecursoInexistenteException.class, () ->
        service.registrarConPersonaExistente(
            DatosDeTest.documentoUnico(), DatosDeTest.emailUnico("nadie")));

    assertTrue(proveedor.emailsCreados.isEmpty());
  }

  @Test
  void siFallaElGuardadoCompensaYNoQuedanNiElRolNiElVinculo() {
    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> personaPersistida(documento));
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(Usuario usuario) {
        throw new RuntimeException("DB caida simulada.");
      }
    };
    DonantesService service = nuevoService(proveedor, repoRoto);

    assertThrows(RuntimeException.class, () ->
        service.registrarConPersonaExistente(documento, DatosDeTest.emailUnico("roto")));

    assertEquals(List.of(kcId), proveedor.eliminados);
    entityManager().clear();
    assertFalse(repoDonantes.buscarPorDocumento(documento).isPresent());
    assertFalse(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
  }

  private PersonaHumana personaPersistida(String documento) {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    return persona;
  }

  private void beneficiariaPersistida(String cuit) {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Benef"), TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    repoEntidades.guardar(new Beneficiaria(juridica));
  }

  private DonantesService nuevoService(ProveedorDeIdentidad proveedor, RepositorioUsuarios usuarios) {
    return new DonantesService(
        repoDonantes, repoPersonas, new RepositorioNotificaciones(),
        RepositorioDonaciones.getInstance(), repoEntidades,
        mock(NotificacionesService.class), new UsuariosService(proveedor, usuarios));
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> emailsCreados = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();
    private final String idFijo;

    ProveedorFalso(String idFijo) {
      this.idFijo = idFijo;
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      emailsCreados.add(email);
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
}
