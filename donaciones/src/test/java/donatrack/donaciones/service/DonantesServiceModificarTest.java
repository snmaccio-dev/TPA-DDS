package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DonantesServiceModificarTest implements WithSimplePersistenceUnit {

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();

  @Test
  void modificarUnDonanteHumanoPersisteLosCambios() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("ana");
    withTransaction(() -> {
      PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
      repoPersonas.guardar(persona);
      repoDonantes.guardar(new Donante(persona));
    });
    entityManager().clear();

    nuevoService().modificar(documento, new DatosDonante(
        "Ana Maria", "Gomez", 31, "otro", null, null, null,
        "Calle Nueva 123", "", "+54 9 11 2222-2222", "WHATSAPP"));

    entityManager().clear();
    PersonaHumana recargada = (PersonaHumana) repoPersonas.buscarPorDocumento(documento).orElseThrow();
    assertEquals("Ana Maria", recargada.getNombre());
    assertEquals("Gomez", recargada.getApellido());
    assertEquals(31, recargada.getEdad());
    assertEquals(Genero.OTRO, recargada.getGenero());
    assertEquals("Calle Nueva 123", recargada.getDireccion());
    assertFalse(recargada.tieneContactoDeTipo(TipoContacto.TELEFONO));
    assertTrue(recargada.tieneContacto(TipoContacto.WHATSAPP, "+54 9 11 2222-2222"));
    assertTrue(recargada.tieneContacto(TipoContacto.EMAIL, email));
    assertEquals(TipoContacto.WHATSAPP, recargada.getContactoPredeterminado().getTipo());
    assertEquals(2, recargada.getContactos().size());
  }

  @Test
  void modificarUnDonanteJuridicoPersisteLosCambiosEIgnoraLosCamposDeHumana() {
    String cuit = DatosDeTest.cuitUnico();
    String email = DatosDeTest.emailUnico("empresa");
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          cuit, DatosDeTest.textoUnico("Empresa"), TipoOrganizacion.EMPRESA, "Rubro");
      juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
      repoPersonas.guardar(juridica);
      repoDonantes.guardar(new Donante(juridica));
    });
    entityManager().clear();

    nuevoService().modificar(cuit, new DatosDonante(
        "ignorado", null, -5, "NO-EXISTE", "Empresa Renovada", "ONG", "Alimentos",
        "Av. Siempre Viva 742", "+54 11 3333-3333", null, "TELEFONO"));

    entityManager().clear();
    PersonaJuridica recargada = repoPersonas.buscarJuridicaPorCuit(cuit).orElseThrow();
    assertEquals("Empresa Renovada", recargada.getRazonSocial());
    assertEquals(TipoOrganizacion.ONG, recargada.getTipo());
    assertEquals("Alimentos", recargada.getRubro());
    assertEquals("Av. Siempre Viva 742", recargada.getDireccion());
    assertEquals(cuit, recargada.getCuit());
    assertTrue(recargada.tieneContacto(TipoContacto.TELEFONO, "+54 11 3333-3333"));
    assertTrue(recargada.tieneContacto(TipoContacto.EMAIL, email));
    assertEquals(TipoContacto.TELEFONO, recargada.getContactoPredeterminado().getTipo());
  }

  @Test
  void unPredeterminadoQueNoQuedaComoContactoDa400YNoPersisteNada() {
    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> {
      PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("a")));
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "111"));
      repoPersonas.guardar(persona);
      repoDonantes.guardar(new Donante(persona));
    });
    entityManager().clear();

    assertThrows(DatosInvalidosException.class, () -> nuevoService().modificar(documento,
        new DatosDonante("Otra", "Persona", 40, null, null, null, null, null,
            null, null, "TELEFONO")));

    entityManager().clear();
    PersonaHumana recargada = (PersonaHumana) repoPersonas.buscarPorDocumento(documento).orElseThrow();
    assertEquals("Ana", recargada.getNombre());
    assertTrue(recargada.tieneContacto(TipoContacto.TELEFONO, "111"));
  }

  @Test
  void unGeneroInvalidoDa400ListandoLosValoresValidos() {
    String documento = DatosDeTest.documentoUnico();
    withTransaction(() -> {
      PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, DatosDeTest.emailUnico("g")));
      repoPersonas.guardar(persona);
      repoDonantes.guardar(new Donante(persona));
    });

    DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () ->
        nuevoService().modificar(documento, new DatosDonante(
            "Ana", "Perez", 30, "ALIEN", null, null, null, null, null, null, null)));

    for (Genero genero : Genero.values()) {
      assertTrue(error.getMessage().contains(genero.name()), error.getMessage());
    }
  }

  @Test
  void unaPersonaQueNoEsDonanteDa404() {
    String cuit = DatosDeTest.cuitUnico();
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          cuit, DatosDeTest.textoUnico("Solo Benef"), TipoOrganizacion.ONG, "Rubro");
      repoPersonas.guardar(juridica);
      repoEntidades.guardar(new Beneficiaria(juridica));
    });

    assertThrows(RecursoInexistenteException.class, () -> nuevoService().modificar(cuit,
        new DatosDonante(null, null, null, null, "X", null, null, null, null, null, null)));
  }

  private DonantesService nuevoService() {
    return new DonantesService(
        repoDonantes,
        repoPersonas,
        new RepositorioNotificaciones(),
        RepositorioDonaciones.getInstance(),
        repoEntidades,
        mock(NotificacionesService.class),
        mock(UsuariosService.class));
  }
}
