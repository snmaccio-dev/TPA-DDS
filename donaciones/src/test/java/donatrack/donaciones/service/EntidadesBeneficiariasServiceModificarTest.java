package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
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
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EntidadesBeneficiariasServiceModificarTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();

  @Test
  void modificarPersisteLosDatosLosRepresentantesNuevosYLosContactos() {
    String cuit = DatosDeTest.cuitUnico();
    String emailViejo = DatosDeTest.emailUnico("rep-viejo");
    String emailQueSigue = DatosDeTest.emailUnico("rep-sigue");
    String emailNuevo = DatosDeTest.emailUnico("rep-nuevo");
    long id = beneficiariaPersistida(cuit, List.of(
        new Representante("Viejo", "Uno", emailViejo),
        new Representante("Sigue", "Dos", emailQueSigue)));
    entityManager().clear();

    nuevoService().modificar(id, "Comedor Renovado", "ong", "Alimentos", new DatosEntidad(
        "Calle Nueva 456", "1199999999", List.of(
            new Representante("Sigue", "Dos", emailQueSigue),
            new Representante("Nueva", "Tres", emailNuevo))));

    entityManager().clear();
    PersonaJuridica recargada = repoEntidades.buscarPorId(id).orElseThrow().getPersona();
    assertEquals("Comedor Renovado", recargada.getRazonSocial());
    assertEquals(TipoOrganizacion.ONG, recargada.getTipo());
    assertEquals("Alimentos", recargada.getRubro());
    assertEquals("Calle Nueva 456", recargada.getDireccion());
    assertEquals(cuit, recargada.getCuit());
    assertEquals(Set.of(emailQueSigue, emailNuevo),
        recargada.getRepresentantes().stream().map(Representante::getEmail)
            .collect(Collectors.toSet()));
    assertTrue(recargada.tieneContacto(TipoContacto.TELEFONO, "1199999999"));
    assertFalse(recargada.tieneContacto(TipoContacto.TELEFONO, "1100000000"));
    assertTrue(recargada.tieneContacto(TipoContacto.EMAIL, emailQueSigue));
    assertTrue(recargada.tieneContacto(TipoContacto.EMAIL, emailNuevo));
    assertFalse(recargada.tieneContacto(TipoContacto.EMAIL, emailViejo));
    assertEquals(TipoContacto.TELEFONO, recargada.getContactoPredeterminado().getTipo());
  }

  @Test
  void unTipoDeOrganizacionInvalidoDa400YNoPersisteNada() {
    String cuit = DatosDeTest.cuitUnico();
    String email = DatosDeTest.emailUnico("rep");
    long id = beneficiariaPersistida(cuit, List.of(new Representante("Rep", "Uno", email)));
    entityManager().clear();

    assertThrows(DatosInvalidosException.class, () ->
        nuevoService().modificar(id, "Otro nombre", "CLUB", null, new DatosEntidad(
            "Otra 1", "123", List.of(new Representante("Rep", "Uno", email)))));

    entityManager().clear();
    assertEquals("Calle Falsa 123",
        repoEntidades.buscarPorId(id).orElseThrow().getPersona().getDireccion());
  }

  @Test
  void unTelefonoViejoQueNoCumpleLasReglasSePuedeConservar() {
    String email = DatosDeTest.emailUnico("rep");
    long id = beneficiariaPersistida(DatosDeTest.cuitUnico(),
        List.of(new Representante("Rep", "Uno", email)));
    // Registro viejo: telefono con un formato que hoy se rechaza.
    withTransaction(() -> repoEntidades.buscarPorId(id).orElseThrow().getPersona()
        .reemplazarContacto(TipoContacto.TELEFONO, "+54 11 0000-0000"));
    entityManager().clear();

    nuevoService().modificar(id, "Comedor Renovado", "ONG", null, new DatosEntidad(
        "Calle Falsa 123", "+54 11 0000-0000", List.of(new Representante("Rep", "Uno", email))));

    entityManager().clear();
    assertEquals("Comedor Renovado",
        repoEntidades.buscarPorId(id).orElseThrow().getPersona().getRazonSocial());
  }

  @Test
  void unTelefonoNuevoQueNoCumpleLasReglasDa400YNoPersisteNada() {
    String email = DatosDeTest.emailUnico("rep");
    long id = beneficiariaPersistida(DatosDeTest.cuitUnico(),
        List.of(new Representante("Rep", "Uno", email)));
    entityManager().clear();

    assertThrows(DatosInvalidosException.class, () ->
        nuevoService().modificar(id, "Comedor Renovado", "ONG", null, new DatosEntidad(
            "Calle Falsa 123", "11-4911-1001", List.of(new Representante("Rep", "Uno", email)))));

    entityManager().clear();
    assertTrue(repoEntidades.buscarPorId(id).orElseThrow().getPersona()
        .tieneContacto(TipoContacto.TELEFONO, "1100000000"));
  }

  @Test
  void unRepresentanteNuevoSeValidaPeroUnoViejoNo() {
    String emailViejo = DatosDeTest.emailUnico("viejo");
    // Representante viejo sin nombre ni apellido: hoy no se podria cargar.
    long id = beneficiariaPersistida(DatosDeTest.cuitUnico(), List.of(new Representante(emailViejo)));
    entityManager().clear();

    assertThrows(DatosInvalidosException.class, () ->
        nuevoService().modificar(id, "Comedor", "ONG", null, new DatosEntidad(
            "Calle Falsa 123", "1100000000", List.of(
                new Representante(emailViejo),
                new Representante("Al", "Nuevo", DatosDeTest.emailUnico("nuevo"))))));

    nuevoService().modificar(id, "Comedor", "ONG", null, new DatosEntidad(
        "Calle Falsa 123", "1100000000", List.of(
            new Representante(emailViejo),
            new Representante("Alba", "Nueva", DatosDeTest.emailUnico("nuevo")))));
  }

  @Test
  void unaEntidadInexistenteDa404() {
    assertThrows(RecursoInexistenteException.class, () ->
        nuevoService().modificar(Long.MAX_VALUE, "X", null, null, new DatosEntidad(
            "Calle 1", "123", List.of(new Representante(DatosDeTest.emailUnico("x"))))));
  }

  private long beneficiariaPersistida(String cuit, List<Representante> representantes) {
    long[] id = new long[1];
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          cuit, DatosDeTest.textoUnico("Comedor"), TipoOrganizacion.INSTITUCION, "Rubro");
      new DatosEntidad("Calle Falsa 123", "1100000000", representantes).aplicarA(juridica);
      repoPersonas.guardar(juridica);
      Beneficiaria beneficiaria = new Beneficiaria(juridica);
      repoEntidades.guardar(beneficiaria);
      id[0] = beneficiaria.getId();
    });
    return id[0];
  }

  private EntidadesBeneficiariasService nuevoService() {
    return new EntidadesBeneficiariasService(
        repoEntidades, repoPersonas, new RepositorioNotificaciones(),
        RepositorioNecesidades.getInstance(), new RepositorioPropuestas(),
        RepositorioDonaciones.getInstance(), new RepositorioDonantes(),
        mock(UsuariosService.class));
  }
}
