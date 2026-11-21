package donatrack.donaciones.service;

import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.Enumerados;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNecesidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.Collection;
import java.util.List;

public class EntidadesBeneficiariasService implements WithSimplePersistenceUnit {

  private final RepositorioEntidades repositorioEntidades;
  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioNotificaciones repositorioNotificaciones;
  private final RepositorioNecesidades repositorioNecesidades;
  private final RepositorioPropuestas repositorioPropuestas;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;
  private final UsuariosService usuariosService;

  public EntidadesBeneficiariasService(RepositorioEntidades repositorioEntidades,
                                       RepositorioPersonas repositorioPersonas,
                                       RepositorioNotificaciones repositorioNotificaciones,
                                       RepositorioNecesidades repositorioNecesidades,
                                       RepositorioPropuestas repositorioPropuestas,
                                       RepositorioDonaciones repositorioDonaciones,
                                       RepositorioDonantes repositorioDonantes,
                                       UsuariosService usuariosService) {
    this.repositorioEntidades = repositorioEntidades;
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioNotificaciones = repositorioNotificaciones;
    this.repositorioNecesidades = repositorioNecesidades;
    this.repositorioPropuestas = repositorioPropuestas;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
    this.usuariosService = usuariosService;
  }

  public List<Beneficiaria> todas() {
    return repositorioEntidades.todas();
  }

  public Beneficiaria buscar(long id) {
    return repositorioEntidades.buscarPorId(id)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe la beneficiaria con id: " + id
            ));
  }

  // Para el mapa publico: de las pedidas, las que tienen coordenadas.
  public List<Beneficiaria> conUbicacion(Collection<Long> ids) {
    return repositorioEntidades.conUbicacion(ids);
  }

  public Beneficiaria buscarPorCuit(String cuit) {
    return repositorioEntidades.buscarPorCuit(cuit)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe la entidad beneficiaria con CUIT " + cuit
        ));
  }

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
      usuariosService.desvincular(beneficiaria).ifPresent(kcId -> keycloakIdAEliminar[0] = kcId);
      repositorioEntidades.eliminar(id);
      if (!personaUsadaPorDonante(juridica)) {
        repositorioPersonas.eliminar(juridica);
      }
    });

    if (keycloakIdAEliminar[0] != null) {
      usuariosService.eliminarEnProveedor(keycloakIdAEliminar[0]);
    }
  }

  public Beneficiaria modificar(long id,
                                String razonSocial,
                                String tipoOrganizacion,
                                String rubro,
                                DatosEntidad datos) {
    exigirDatos(datos);
    TipoOrganizacion tipo =
        Enumerados.desde(TipoOrganizacion.class, tipoOrganizacion, "tipoOrganizacion");
    Beneficiaria[] modificada = new Beneficiaria[1];
    try {
      withTransaction(() -> {
        modificada[0] = buscar(id);
        PersonaJuridica juridica = modificada[0].getPersona();
        String direccionAnterior = juridica.getDireccion();
        juridica.actualizarDatos(razonSocial, tipo, rubro);
        datos.reemplazarEn(juridica);
        modificada[0].olvidarUbicacionSiCambioLaDireccion(direccionAnterior);
      });
    } catch (RuntimeException e) {
      entityManager().clear();
      throw e;
    }
    return modificada[0];
  }

  public Beneficiaria registrarConPersonaExistente(String cuit, DatosEntidad datos) {
    return registrarConPersonaExistente(cuit, datos, null);
  }

  // ubicacion es opcional (null): si viene, la entidad no se geocodifica.
  public Beneficiaria registrarConPersonaExistente(String cuit,
                                                   DatosEntidad datos,
                                                   Coordenadas ubicacion) {
    exigirDatos(datos);
    PersonaJuridica juridica = repositorioPersonas.buscarJuridicaPorCuit(cuit)
        .orElseThrow(() -> new RecursoInexistenteException(
            "No existe una persona juridica con CUIT " + cuit
        ));
    if (repositorioEntidades.buscarPorCuit(juridica.getCuit()).isPresent()) {
      throw new ConflictoDeNegocioException("La entidad ya es beneficiaria.");
    }
    String email = emailDeUnRepresentante(datos);

    UsuariosService.CuentaObtenida cuenta =
        usuariosService.obtenerCuenta(email, RolDeAcceso.BENEFICIARIA, true);
    String keycloakId = cuenta.keycloakId();
    Beneficiaria[] creada = new Beneficiaria[1];
    try {
      withTransaction(() -> {
        datos.aplicarA(juridica);
        creada[0] = guardarBeneficiaria(juridica, ubicacion);
        usuariosService.vincular(keycloakId, creada[0]);
      });
    } catch (RuntimeException e) {
      if (cuenta.creada()) {
        usuariosService.compensar(keycloakId);
      }
      throw e;
    }
    return creada[0];
  }

  public Beneficiaria registrarConPersonaNueva(PersonaJuridica juridica, DatosEntidad datos) {
    return registrarConPersonaNueva(juridica, datos, null);
  }

  // ubicacion es opcional (null): si viene, la entidad no se geocodifica.
  public Beneficiaria registrarConPersonaNueva(PersonaJuridica juridica,
                                               DatosEntidad datos,
                                               Coordenadas ubicacion) {
    exigirDatos(datos);
    if (repositorioPersonas.buscarPorDocumento(juridica.getCuit()).isPresent()) {
      throw new ConflictoDeNegocioException(
          "Ya existe una persona con ese CUIT. Si se la quiere hacer beneficiaria, "
              + "usa el alta con persona existente.");
    }
    String email = emailDeUnRepresentante(datos);

    UsuariosService.CuentaObtenida cuenta =
        usuariosService.obtenerCuenta(email, RolDeAcceso.BENEFICIARIA, true);
    String keycloakId = cuenta.keycloakId();
    Beneficiaria[] creada = new Beneficiaria[1];
    try {
      withTransaction(() -> {
        creada[0] = crearPersonaYBeneficiaria(juridica, datos, ubicacion);
        usuariosService.vincular(keycloakId, creada[0]);
      });
    } catch (RuntimeException e) {
      if (cuenta.creada()) {
        usuariosService.compensar(keycloakId);
      }
      throw e;
    }
    return creada[0];
  }

  public Beneficiaria registrarPublica(PersonaJuridica juridica,
                                       DatosEntidad datos,
                                       String email,
                                       String contrasena) {
    UsuariosService.validarCredenciales(email, contrasena);
    if (juridica == null) {
      throw new DatosInvalidosException("Los datos de la persona juridica son obligatorios.");
    }
    exigirDatos(datos);
    if (repositorioPersonas.buscarPorDocumento(juridica.getCuit()).isPresent()) {
      throw new ConflictoDeNegocioException(UsuariosService.MENSAJE_PERSONA_DUPLICADA);
    }

    String keycloakId =
        usuariosService.crearEnProveedor(email, contrasena, RolDeAcceso.BENEFICIARIA);
    Beneficiaria[] creada = new Beneficiaria[1];
    try {
      withTransaction(() -> {
        juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
        creada[0] = crearPersonaYBeneficiaria(juridica, datos, null);
        usuariosService.vincular(keycloakId, creada[0]);
      });
    } catch (RuntimeException e) {
      usuariosService.compensar(keycloakId);
      throw e;
    }
    return creada[0];
  }

  private Beneficiaria crearPersonaYBeneficiaria(PersonaJuridica juridica,
                                                 DatosEntidad datos,
                                                 Coordenadas ubicacion) {
    datos.aplicarA(juridica);
    repositorioPersonas.guardar(juridica);
    return guardarBeneficiaria(juridica, ubicacion);
  }

  private Beneficiaria guardarBeneficiaria(PersonaJuridica juridica, Coordenadas ubicacion) {
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    beneficiaria.ubicarEn(ubicacion);
    repositorioEntidades.guardar(beneficiaria);
    return beneficiaria;
  }

  private static String emailDeUnRepresentante(DatosEntidad datos) {
    List<Representante> representantes =
        datos.representantes() == null ? List.of() : datos.representantes();
    return representantes.stream()
        .map(Representante::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .findFirst()
        .orElseThrow(() -> new DatosInvalidosException(
            "La beneficiaria debe indicar al menos un representante con su correo."));
  }

  private static void exigirDatos(DatosEntidad datos) {
    if (datos == null) {
      throw new DatosInvalidosException(
          "Los datos de la beneficiaria (direccion y telefono) son obligatorios."
      );
    }
  }

  private boolean personaUsadaPorDonante(PersonaJuridica juridica) {
    return repositorioDonantes.buscarPorPersona(juridica).isPresent();
  }
}
