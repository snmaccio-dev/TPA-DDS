package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.domain.persona.Donante;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntidadesBeneficiariasServiceAltaTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void conPersonaNuevaCreaPersonaBeneficiariaYVinculo() {
    String cuit = DatosDeTest.cuitUnico();
    String emailRep = DatosDeTest.emailUnico("rep-nuevo");
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    Beneficiaria creada = service.registrarConPersonaNueva(juridicaNueva(cuit), datos(emailRep));

    assertEquals(List.of(emailRep), proveedor.emailsCreados);
    entityManager().clear();
    assertTrue(repoEntidades.buscarPorCuit(cuit).isPresent());
    assertEquals(creada.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow().getBeneficiaria().getId());
  }

  @Test
  void conUnTelefonoConSimbolosDa400SinLlamarAKeycloak() {
    String cuit = DatosDeTest.cuitUnico();
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        service.registrarConPersonaNueva(juridicaNueva(cuit), new DatosEntidad(
            "Calle Falsa 123", "+54 11 4911-1001",
            List.of(new Representante("Rep", "Uno", DatosDeTest.emailUnico("rep"))))));

    assertEquals("El teléfono tiene que tener entre 8 y 15 dígitos, solo números.",
        error.getMessage());
    assertTrue(proveedor.emailsCreados.isEmpty());
    entityManager().clear();
    assertFalse(repoEntidades.buscarPorCuit(cuit).isPresent());
  }

  @Test
  void conUnRepresentanteSinApellidoValidoDa400() {
    EntidadesBeneficiariasService service =
        nuevoService(new ProveedorFalso(DatosDeTest.keycloakIdUnico()), repoUsuarios);

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        service.registrarConPersonaNueva(juridicaNueva(DatosDeTest.cuitUnico()), new DatosEntidad(
            "Calle Falsa 123", "1100000000",
            List.of(new Representante("Rep", "U2", DatosDeTest.emailUnico("rep"))))));

    assertTrue(error.getMessage().startsWith("El apellido del representante"), error.getMessage());
  }

  @Test
  void conPersonaNuevaYCoordenadasLaEntidadQuedaUbicada() {
    String cuit = DatosDeTest.cuitUnico();
    EntidadesBeneficiariasService service =
        nuevoService(new ProveedorFalso(DatosDeTest.keycloakIdUnico()), repoUsuarios);

    service.registrarConPersonaNueva(juridicaNueva(cuit),
        datos(DatosDeTest.emailUnico("rep-ubicada")), new Coordenadas(-34.5838, -58.3930));

    entityManager().clear();
    assertEquals(Optional.of(new Coordenadas(-34.5838, -58.3930)),
        repoEntidades.buscarPorCuit(cuit).orElseThrow().getUbicacion());
  }

  @Test
  void conPersonaNuevaSinCoordenadasLaEntidadQuedaSinUbicar() {
    String cuit = DatosDeTest.cuitUnico();
    EntidadesBeneficiariasService service =
        nuevoService(new ProveedorFalso(DatosDeTest.keycloakIdUnico()), repoUsuarios);

    service.registrarConPersonaNueva(juridicaNueva(cuit), datos(DatosDeTest.emailUnico("rep")));

    entityManager().clear();
    assertFalse(repoEntidades.buscarPorCuit(cuit).orElseThrow().tieneUbicacion());
  }

  @Test
  void conPersonaNuevaCuyoCuitYaExisteDa409SinLlamarAKeycloak() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> repoPersonas.guardar(juridicaNueva(cuit)));
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    String cuitConGuiones = cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10);
    assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarConPersonaNueva(
            juridicaNueva(cuitConGuiones), datos(DatosDeTest.emailUnico("dup"))));

    assertTrue(proveedor.emailsCreados.isEmpty());
    entityManager().clear();
    assertFalse(repoEntidades.buscarPorCuit(cuit).isPresent());
  }

  @Test
  void conPersonaExistenteQueNoEsBeneficiariaCreaElRolYLaCuenta() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> {
      PersonaJuridica juridica = juridicaNueva(cuit);
      repoPersonas.guardar(juridica);
      repoDonantes.guardar(new Donante(juridica));
    });
    String emailRep = DatosDeTest.emailUnico("rep-existente");
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    Beneficiaria creada = service.registrarConPersonaExistente(cuit, datos(emailRep));

    assertEquals(List.of(emailRep), proveedor.emailsCreados);
    entityManager().clear();
    Beneficiaria refrescada = repoEntidades.buscarPorCuit(cuit).orElseThrow();
    assertEquals("Calle Falsa 123", refrescada.getPersona().getDireccion());
    assertEquals(creada.getId(),
        repoUsuarios.buscarPorKeycloakId(kcId).orElseThrow().getBeneficiaria().getId());
  }

  @Test
  void conPersonaExistenteQueYaEsBeneficiariaDa409SinLlamarAKeycloak() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> {
      PersonaJuridica juridica = juridicaNueva(cuit);
      repoPersonas.guardar(juridica);
      Beneficiaria beneficiaria = new Beneficiaria(juridica);
      repoEntidades.guardar(beneficiaria);
      repoUsuarios.guardar(Usuario.deBeneficiaria(DatosDeTest.keycloakIdUnico(), beneficiaria));
    });
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    ConflictoDeNegocioException error = assertThrows(ConflictoDeNegocioException.class, () ->
        service.registrarConPersonaExistente(cuit, datos(DatosDeTest.emailUnico("ya-benef"))));

    assertEquals("La entidad ya es beneficiaria.", error.getMessage());
    assertTrue(proveedor.emailsCreados.isEmpty());
  }

  @Test
  void conUnCuitInexistenteDa404() {
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    assertThrows(RecursoInexistenteException.class, () ->
        service.registrarConPersonaExistente(
            DatosDeTest.cuitUnico(), datos(DatosDeTest.emailUnico("nadie"))));

    assertTrue(proveedor.emailsCreados.isEmpty());
  }

  @Test
  void sinRepresentantesConEmailDa400SinLlamarAKeycloak() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> repoPersonas.guardar(juridicaNueva(cuit)));
    ProveedorFalso proveedor = new ProveedorFalso(DatosDeTest.keycloakIdUnico());
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoUsuarios);

    assertThrows(DatosInvalidosException.class, () ->
        service.registrarConPersonaExistente(
            cuit, new DatosEntidad("Calle Falsa 123", "1100000000", List.of())));

    assertTrue(proveedor.emailsCreados.isEmpty());
    entityManager().clear();
    assertFalse(repoEntidades.buscarPorCuit(cuit).isPresent());
  }

  @Test
  void siFallaElGuardadoCompensaYNoQuedanNiElRolNiElVinculo() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> repoPersonas.guardar(juridicaNueva(cuit)));
    String kcId = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso(kcId);
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(Usuario usuario) {
        throw new RuntimeException("DB caida simulada.");
      }
    };
    EntidadesBeneficiariasService service = nuevoService(proveedor, repoRoto);

    assertThrows(RuntimeException.class, () ->
        service.registrarConPersonaExistente(cuit, datos(DatosDeTest.emailUnico("roto"))));

    assertEquals(List.of(kcId), proveedor.eliminados);
    entityManager().clear();
    assertFalse(repoEntidades.buscarPorCuit(cuit).isPresent());
    assertFalse(repoUsuarios.buscarPorKeycloakId(kcId).isPresent());
  }

  private PersonaJuridica juridicaNueva(String cuit) {
    return new PersonaJuridica(cuit, DatosDeTest.textoUnico("Benef"), TipoOrganizacion.ONG, "Rubro");
  }

  private DatosEntidad datos(String emailRepresentante) {
    return new DatosEntidad(
        "Calle Falsa 123",
        "1100000000",
        List.of(new Representante("Rep", "Uno", emailRepresentante))
    );
  }

  private EntidadesBeneficiariasService nuevoService(ProveedorDeIdentidad proveedor,
                                                     RepositorioUsuarios usuarios) {
    return new EntidadesBeneficiariasService(
        repoEntidades, repoPersonas, new RepositorioNotificaciones(),
        RepositorioNecesidades.getInstance(), new RepositorioPropuestas(),
        RepositorioDonaciones.getInstance(), repoDonantes,
        new UsuariosService(proveedor, usuarios));
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
