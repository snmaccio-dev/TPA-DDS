package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
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
  private final RepositorioNotificaciones repositorioNotificaciones;
  private final RepositorioNecesidades repositorioNecesidades;
  private final RepositorioPropuestas repositorioPropuestas;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;
  private final ProveedorDeIdentidad proveedor;

  public GestorEntidadesBeneficiarias(RepositorioUsuarios repositorioUsuarios,
                                      RepositorioNotificaciones repositorioNotificaciones,
                                      RepositorioNecesidades repositorioNecesidades,
                                      RepositorioPropuestas repositorioPropuestas,
                                      RepositorioDonaciones repositorioDonaciones,
                                      RepositorioDonantes repositorioDonantes,
                                      ProveedorDeIdentidad proveedor) {
    this.repositorioUsuarios = repositorioUsuarios;
    this.repositorioNotificaciones = repositorioNotificaciones;
    this.repositorioNecesidades = repositorioNecesidades;
    this.repositorioPropuestas = repositorioPropuestas;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
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
  // Borrado en cascada de una beneficiaria. Contrato:
  // 1. 404 si no existe.
  // 2. 409 si alguna donacion la tiene como destinatarioAsignado (cualquier
  //    estado), para conservar la trazabilidad.
  // 3. En una unica transaccion: notificaciones, la saca de las candidatas
  //    de las propuestas sin borrarlas, borra sus necesidades, su usuario,
  //    la beneficiaria y la persona juridica (si ningun donante la usa).
  // 4. Despues del commit, borra la cuenta en Keycloak. Si Keycloak falla
  //    solo se loguea.
  public void eliminar(long id) {
    Beneficiaria beneficiaria = buscar(id);

    if (repositorioDonaciones.tieneDonacionesAsignadasA(beneficiaria)) {
      throw new ConflictoDeNegocioException(
          "La entidad tiene donaciones asignadas y no puede eliminarse para "
              + "conservar la trazabilidad.");
    }

    String[] keycloakIdAEliminar = new String[1];

    withTransaction(() -> {
      PersonaJuridica juridica = beneficiaria.getPersona();
      repositorioNotificaciones.eliminarDeBeneficiaria(beneficiaria);
      repositorioPropuestas.quitarComoCandidata(beneficiaria);
      repositorioNecesidades.eliminarDeBeneficiaria(beneficiaria);
      Optional<Usuario> usuario = repositorioUsuarios.deBeneficiaria(beneficiaria);
      usuario.ifPresent(u -> {
        keycloakIdAEliminar[0] = u.getKeycloakId();
        repositorioUsuarios.eliminar(u);
      });
      repositorioEntidades.eliminar(id);
      if (!personaUsadaPorDonante(juridica)) {
        repositorioPersonas.eliminar(juridica);
      }
    });

    if (keycloakIdAEliminar[0] != null) {
      intentarEliminar(keycloakIdAEliminar[0]);
    }
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
    // Cada perfil tiene una unica cuenta: si ya hay una, la reutilizamos tal
    // cual. El admin puede re-enviar el alta sobre una beneficiaria que ya
    // esta registrada sin que esto sea un error.
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
      intentarEliminar(keycloakId);
      throw e;
    }
  }

  private boolean personaUsadaPorDonante(PersonaJuridica juridica) {
    return repositorioDonantes.buscarPorPersona(juridica).isPresent();
  }

  private void intentarEliminar(String keycloakId) {
    try {
      proveedor.eliminarUsuario(keycloakId);
    } catch (RuntimeException ignorado) {
      System.err.println("[ENTIDADES] Rollback en Keycloak fallo para " + keycloakId
          + ": " + ignorado.getMessage());
    }
  }
}
