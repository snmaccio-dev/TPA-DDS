package donatrack.donaciones.service;

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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorEntidadesBeneficiariasAltaTest implements SimplePersistenceTest {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();
  private final RepositorioNotificaciones repoNotificaciones = new RepositorioNotificaciones();
  private final RepositorioNecesidades repoNecesidades = RepositorioNecesidades.getInstance();
  private final RepositorioPropuestas repoPropuestas = new RepositorioPropuestas();
  private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();

  @Test
  void registrarDePersonaExistenteYaConCuentaNoLlamaAKeycloak() {
    String cuit = "30-11111111-1";
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, "Benef X", TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    repoEntidades.guardar(beneficiaria);
    repoUsuarios.guardar(Usuario.deBeneficiaria("kc-benef", beneficiaria));
    entityManager().flush();
    entityManager().clear();

    ProveedorFalso proveedor = new ProveedorFalso();
    GestorEntidadesBeneficiarias gestor = new GestorEntidadesBeneficiarias(
        repoUsuarios, repoNotificaciones, repoNecesidades, repoPropuestas,
        repoDonaciones, repoDonantes, proveedor);

    DatosEntidad datos = new DatosEntidad(
        "Nueva direccion 123",
        "+54 11 0000-0000",
        List.of(new Representante("Rep", "Uno", "rep@mail.com"))
    );

    assertDoesNotThrow(() -> gestor.registrarDePersonaExistente(cuit, datos));

    assertTrue(proveedor.emailsCreados.isEmpty(),
        "No se tiene que llamar a Keycloak si la beneficiaria ya tenia cuenta.");
    // Y los datos nuevos quedaron aplicados como en cualquier alta exitosa.
    entityManager().clear();
    Beneficiaria refrescada = repoEntidades.buscarPorCuit(juridica.getCuit()).orElseThrow();
    assertEquals("Nueva direccion 123", refrescada.getPersona().getDireccion());
  }

  @Test
  void registrarConPersonaNuevaCreaLaCuentaCuandoLaBeneficiariaEsNueva() {
    ProveedorFalso proveedor = new ProveedorFalso("kc-alta");
    GestorEntidadesBeneficiarias gestor = new GestorEntidadesBeneficiarias(
        repoUsuarios, repoNotificaciones, repoNecesidades, repoPropuestas,
        repoDonaciones, repoDonantes, proveedor);

    PersonaJuridica juridica = new PersonaJuridica(
        "30-22222222-2", "Nueva Benef", TipoOrganizacion.ONG, "Rubro");
    DatosEntidad datos = new DatosEntidad(
        "Direccion",
        "+54 11 0000-1111",
        List.of(new Representante("Rep", "Nuevo", "rep.nuevo@mail.com"))
    );

    Beneficiaria creada = gestor.registrarConPersonaNueva(juridica, datos);

    assertEquals(1, proveedor.emailsCreados.size());
    assertEquals("rep.nuevo@mail.com", proveedor.emailsCreados.get(0));
    assertTrue(repoUsuarios.existeParaBeneficiaria(creada));
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> emailsCreados = new ArrayList<>();
    private final String idFijo;

    ProveedorFalso() { this(null); }
    ProveedorFalso(String idFijo) { this.idFijo = idFijo; }

    @Override public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      emailsCreados.add(email);
      return idFijo != null ? idFijo : "kc-" + email;
    }

    @Override public void eliminarUsuario(String idExterno) {
      // no-op
    }
  }
}
