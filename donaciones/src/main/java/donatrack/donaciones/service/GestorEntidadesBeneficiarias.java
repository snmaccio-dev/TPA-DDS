package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Alta desde un administrador. Si la beneficiaria todavia no tiene ningun
// Usuario, se crea una cuenta en Keycloak (sin contrasena) para el email del
// primer representante, con el rol BENEFICIARIA.
public class GestorEntidadesBeneficiarias implements WithSimplePersistenceUnit {

  private final RepositorioEntidades repositorioEntidades =
      RepositorioEntidades.getInstance();

  private final RepositorioPersonas repositorioPersonas =
      RepositorioPersonas.getInstance();

  private final RepositorioUsuarios repositorioUsuarios;
  private final ProveedorDeIdentidad proveedor;

  public GestorEntidadesBeneficiarias(RepositorioUsuarios repositorioUsuarios,
                                      ProveedorDeIdentidad proveedor) {
    this.repositorioUsuarios = repositorioUsuarios;
    this.proveedor = proveedor;
  }

  // GET /entidades
  public List<Beneficiaria> todas() {
    return repositorioEntidades.todas();
  }

  // GET /entidades/{id}
  public Beneficiaria buscar(long id) {
    return repositorioEntidades.buscarPorId(id)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe la beneficiaria con id: " + id
            ));
  }

  // DELETE /entidades/{id}
  public void eliminar(long id) {
    buscar(id);
    withTransaction(() -> repositorioEntidades.eliminar(id));
  }

  public void actualizarDireccion(long id, String nuevaDireccion) {
    if (nuevaDireccion == null || nuevaDireccion.isBlank()) {
      throw new DatosInvalidosException("La direccion de la beneficiaria no puede quedar vacia.");
    }
    withTransaction(() -> {
      Beneficiaria beneficiaria = buscar(id);
      beneficiaria.getPersona().setDireccion(nuevaDireccion.trim());
    });
  }

  public Optional<PersonaJuridica> buscarPorCuit(String cuit) {
    return repositorioPersonas.buscarJuridicaPorCuit(cuit);
  }

  // Ya no decide si se escriben datos: sirve al front para saber qué campos precargar.
  public List<String> datosFaltantesParaBeneficiaria(PersonaJuridica juridica) {
    List<String> faltantes = new ArrayList<>();
    if (juridica.getDireccion() == null || juridica.getDireccion().isBlank()) {
      faltantes.add("direccion");
    }
    if (!juridica.tieneContactoDeTipo(TipoContacto.TELEFONO)) {
      faltantes.add("telefono");
    }
    return faltantes;
  }

  public Beneficiaria registrarDePersonaExistente(String cuit, DatosEntidad datos) {
    if (datos == null) {
      throw new DatosInvalidosException(
          "Los datos de la beneficiaria (direccion y telefono) son obligatorios."
      );
    }
    Beneficiaria[] resultado = new Beneficiaria[1];
    withTransaction(() -> {
      PersonaJuridica juridica = repositorioPersonas.buscarJuridicaPorCuit(cuit)
          .orElseThrow(() -> new RecursoInexistenteException(
              "No existe una persona juridica con CUIT " + cuit
          ));
      datos.aplicarA(juridica);
      resultado[0] = crearBeneficiaria(juridica);
    });
    asegurarCuenta(resultado[0], datos.representantes());
    return resultado[0];
  }

  public Beneficiaria registrarConPersonaNueva(PersonaJuridica juridica, DatosEntidad datos) {
    if (datos == null) {
      throw new DatosInvalidosException(
          "Los datos de la beneficiaria (direccion y telefono) son obligatorios."
      );
    }
    Beneficiaria[] resultado = new Beneficiaria[1];
    withTransaction(() -> {
      repositorioPersonas.guardar(juridica);
      datos.aplicarA(juridica);
      resultado[0] = crearBeneficiaria(juridica);
    });
    asegurarCuenta(resultado[0], datos.representantes());
    return resultado[0];
  }

  private Beneficiaria crearBeneficiaria(PersonaJuridica juridica) {
    return repositorioEntidades.buscarPorCuit(juridica.getCuit())
        .orElseGet(() -> {
          Beneficiaria beneficiaria = new Beneficiaria(juridica);
          repositorioEntidades.guardar(beneficiaria);
          return beneficiaria;
        });
  }

  // Si la beneficiaria todavia no tiene ningun Usuario vinculado, le damos de
  // alta una cuenta en Keycloak para el email del primer representante y
  // persistimos el vinculo. Si falla el guardado despues del alta, hacemos
  // rollback contra Keycloak.
  private void asegurarCuenta(Beneficiaria beneficiaria, List<Representante> representantes) {
    if (repositorioUsuarios.existeParaBeneficiaria(beneficiaria)) {
      return;
    }
    if (representantes == null || representantes.isEmpty()) {
      return;
    }
    String email = representantes.get(0).getEmail();
    if (email == null || email.isBlank()) {
      return;
    }
    String keycloakId = proveedor.crearUsuarioSinContrasena(email, RolDeAcceso.BENEFICIARIA);
    try {
      withTransaction(() ->
          repositorioUsuarios.guardar(Usuario.deBeneficiaria(keycloakId, beneficiaria))
      );
    } catch (RuntimeException e) {
      try {
        proveedor.eliminarUsuario(keycloakId);
      } catch (RuntimeException ignorado) {
        System.err.println("[ENTIDADES] Rollback en Keycloak fallo para " + keycloakId
            + ": " + ignorado.getMessage());
      }
      throw e;
    }
  }
}