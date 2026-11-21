package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.importacion.ErrorDeImportacion;
import donatrack.donaciones.domain.importacion.EstadoImportacion;
import donatrack.donaciones.domain.importacion.ImportacionCsv;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.infrastructure.importacion.ImportacionCsvException;
import donatrack.donaciones.infrastructure.importacion.LectorCsvDonantes;
import donatrack.donaciones.infrastructure.importacion.LineaCsv;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioImportaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.IntegracionConIdentidadException;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportacionDonantesServiceTest implements WithSimplePersistenceUnit {

  private static final String ENCABEZADO = "TipoPersona,TipoDoc,Documento,NombreRazonSocial,Email,Telefono";

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();
  private final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

  @Test
  void unaPersonaNuevaCreaPersonaDonanteYCuentaSinMandarMail() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("nueva");
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor,
        csv("HUMANA,DNI," + documento + ",Nora Diaz," + email + ",1111111111"));

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertEquals(1, resultado.getCreados());
    assertEquals(1, resultado.getCuentasCreadas());
    assertEquals(List.of(email), proveedor.creados);
    assertEquals(List.of(false), proveedor.conMail);

    Donante donante = repoDonantes.buscarPorDocumento(documento).orElseThrow();
    PersonaHumana persona = (PersonaHumana) donante.getPersona();
    assertEquals("Nora", persona.getNombre());
    assertEquals("Diaz", persona.getApellido());
    assertTrue(persona.tieneContacto(TipoContacto.EMAIL, email));
    assertTrue(persona.tieneContacto(TipoContacto.TELEFONO, "1111111111"));
    assertEquals(donante.getId(),
        repoUsuarios.buscarPorKeycloakId(proveedor.idDe(email)).orElseThrow().getDonante().getId());
  }

  @Test
  void unaPersonaExistentePorEmailPisaRazonSocialYTelefonoSinTocarDireccionNiRepresentantes() {
    String cuit = DatosDeTest.cuitUnico();
    String email = DatosDeTest.emailUnico("juridica");
    String emailRepresentante = DatosDeTest.emailUnico("rep");
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          cuit, DatosDeTest.textoUnico("Vieja"), TipoOrganizacion.ONG, "Rubro");
      juridica.setDireccion("Calle Vieja 123");
      juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
      juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, "1100000000"));
      juridica.agregarRepresentante(new Representante("Rep", "Uno", emailRepresentante));
      repoPersonas.guardar(juridica);
      Donante donante = new Donante(juridica);
      repoDonantes.guardar(donante);
      repoUsuarios.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));
    });
    String cuitConGuiones = cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10);
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor,
        csv("JURIDICA,CUIT," + cuitConGuiones + ",Razon Nueva S.A.," + email + ",1122222222"));

    assertEquals(1, resultado.getActualizados());
    assertEquals(0, resultado.getCreados());
    assertTrue(proveedor.creados.isEmpty(), "Ya tenia cuenta: no se llama a Keycloak.");
    PersonaJuridica actualizada = repoPersonas.buscarJuridicaPorCuit(cuit).orElseThrow();
    assertEquals("Razon Nueva S.A.", actualizada.getRazonSocial());
    assertEquals("Calle Vieja 123", actualizada.getDireccion());
    assertEquals(1, actualizada.getRepresentantes().size());
    assertEquals(emailRepresentante, actualizada.getRepresentantes().get(0).getEmail());
    assertEquals(List.of("1122222222"), telefonosDe(actualizada));
  }

  @Test
  void unaPersonaExistentePorDocumentoConOtroEmailAgregaElEmailYActualiza() {
    String documento = DatosDeTest.documentoUnico();
    String emailViejo = DatosDeTest.emailUnico("viejo");
    String emailNuevo = DatosDeTest.emailUnico("nuevo");
    withTransaction(() -> {
      PersonaHumana persona = new PersonaHumana("Luis", "Garcia", 40, documento, Genero.MASCULINO);
      persona.setDireccion("Av. Siempre Viva 742");
      persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, emailViejo));
      repoPersonas.guardar(persona);
      Donante donante = new Donante(persona);
      repoDonantes.guardar(donante);
      repoUsuarios.guardar(Usuario.deDonante(DatosDeTest.keycloakIdUnico(), donante));
    });
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor,
        csv("HUMANA,DNI," + documento + ",Luis Alberto Garcia," + emailNuevo + ",1133333333"));

    assertEquals(1, resultado.getActualizados());
    assertEquals(0, resultado.getFilasConError());
    PersonaHumana persona = (PersonaHumana) repoPersonas.buscarPorDocumento(documento).orElseThrow();
    assertEquals("Luis", persona.getNombre());
    assertEquals("Alberto Garcia", persona.getApellido());
    assertEquals(40, persona.getEdad());
    assertEquals("Av. Siempre Viva 742", persona.getDireccion());
    assertTrue(persona.tieneContacto(TipoContacto.EMAIL, emailViejo));
    assertTrue(persona.tieneContacto(TipoContacto.EMAIL, emailNuevo));
    assertEquals(List.of("1133333333"), telefonosDe(persona));
  }

  @Test
  void unaPersonaExistenteQueNoEraDonantePasaASerloConCuenta() {
    String cuit = DatosDeTest.cuitUnico();
    String email = DatosDeTest.emailUnico("benef");
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          cuit, DatosDeTest.textoUnico("Benef"), TipoOrganizacion.ONG, "Rubro");
      juridica.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
      repoPersonas.guardar(juridica);
      repoEntidades.guardar(new Beneficiaria(juridica));
    });
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor,
        csv("JURIDICA,CUIT," + cuit + ",Benef Donante," + email + ","));

    assertEquals(1, resultado.getActualizados());
    assertEquals(1, resultado.getCuentasCreadas());
    Donante donante = repoDonantes.buscarPorDocumento(cuit).orElseThrow();
    assertTrue(repoEntidades.buscarPorCuit(cuit).isPresent(), "Sigue siendo beneficiaria.");
    assertEquals(donante.getId(),
        repoUsuarios.buscarPorKeycloakId(proveedor.idDe(email)).orElseThrow().getDonante().getId());
  }

  @Test
  void unDonanteQueYaTieneCuentaNoLlamaAKeycloak() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("con-cuenta");
    withTransaction(() -> donanteConCuenta(documento, email));
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor,
        csv("HUMANA,DNI," + documento + ",Ana Perez," + email + ","));

    assertTrue(proveedor.creados.isEmpty());
    assertTrue(proveedor.busquedas.isEmpty());
    assertEquals(0, resultado.getCuentasCreadas());
    assertEquals(0, resultado.getCuentasVinculadas());
    assertEquals(1, resultado.getActualizados());
  }

  @Test
  void siKeycloakDiceQueElEmailYaTieneCuentaSeBuscaElIdYSeVincula() {
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("reimportado");
    String idExistente = DatosDeTest.keycloakIdUnico();
    ProveedorFalso proveedor = new ProveedorFalso();
    proveedor.existentes.put(email, idExistente);

    ImportacionCsv resultado = importar(proveedor,
        csv("HUMANA,DNI," + documento + ",Ana Perez," + email + ","));

    assertEquals(1, resultado.getCreados());
    assertEquals(0, resultado.getCuentasCreadas());
    assertEquals(1, resultado.getCuentasVinculadas());
    assertEquals(List.of(email), proveedor.busquedas);
    Donante donante = repoDonantes.buscarPorDocumento(documento).orElseThrow();
    assertEquals(donante.getId(),
        repoUsuarios.buscarPorKeycloakId(idExistente).orElseThrow().getDonante().getId());
  }

  @Test
  void siLaCuentaDeKeycloakYaEstaVinculadaAOtroPerfilEsErrorDeFila() {
    String idAjeno = DatosDeTest.keycloakIdUnico();
    withTransaction(() ->
        donanteConCuenta(DatosDeTest.documentoUnico(), DatosDeTest.emailUnico("otro"), idAjeno));
    String documento = DatosDeTest.documentoUnico();
    String email = DatosDeTest.emailUnico("ajeno");
    ProveedorFalso proveedor = new ProveedorFalso();
    proveedor.existentes.put(email, idAjeno);

    ImportacionCsv resultado = importar(proveedor,
        csv("HUMANA,DNI," + documento + ",Ana Perez," + email + ","));

    assertEquals(1, resultado.getFilasConError());
    assertTrue(resultado.getErrores().get(0).getMensaje().contains("vinculado a otro perfil"));
    assertFalse(repoDonantes.buscarPorDocumento(documento).isPresent());
  }

  @Test
  void unaFilaQueFallaNoImpideProcesarLasDemasYQuedaConSuNumero() {
    String docA = DatosDeTest.documentoUnico();
    String docB = DatosDeTest.documentoUnico();
    String docRechazado = DatosDeTest.documentoUnico();
    String emailRechazado = DatosDeTest.emailUnico("rechazado");
    ProveedorFalso proveedor = new ProveedorFalso();
    proveedor.rechazados.add(emailRechazado);

    ImportacionCsv resultado = importar(proveedor, csv(
        "HUMANA,DNI," + docA + ",Ana Uno," + DatosDeTest.emailUnico("a") + ",",
        "MARCIANA,DNI," + DatosDeTest.documentoUnico() + ",Nadie," + DatosDeTest.emailUnico("x") + ",",
        "HUMANA,DNI," + docRechazado + ",Rechazada Uno," + emailRechazado + ",",
        "HUMANA,DNI," + docB + ",Bruno Dos," + DatosDeTest.emailUnico("b") + ","));

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertEquals(2, resultado.getCreados());
    assertEquals(2, resultado.getFilasConError());
    assertEquals(List.of(3, 4), numerosDeFila(resultado));
    assertTrue(resultado.getErrores().get(1).getMensaje().contains("invalid email"),
        "El error de Keycloak tiene que quedar con su mensaje.");
    assertTrue(repoDonantes.buscarPorDocumento(docA).isPresent());
    assertTrue(repoDonantes.buscarPorDocumento(docB).isPresent());
    assertFalse(repoPersonas.buscarPorDocumento(docRechazado).isPresent());
  }

  @Test
  void siFallaLaBaseEnUnaFilaSeReintentaLaTandaFilaPorFilaYSeCompensan() {
    String docA = DatosDeTest.documentoUnico();
    String docB = DatosDeTest.documentoUnico();
    String docRoto = DatosDeTest.documentoUnico();
    String emailRoto = DatosDeTest.emailUnico("roto");
    ProveedorFalso proveedor = new ProveedorFalso();
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(Usuario usuario) {
        if (emailRoto.equals(proveedor.emailDe(usuario.getKeycloakId()))) {
          throw new RuntimeException("DB caida simulada.");
        }
        super.guardar(usuario);
      }
    };

    ImportacionCsv resultado = importar(nuevoService(proveedor, repoRoto, new LectorCsvDonantes()), csv(
        "HUMANA,DNI," + docA + ",Ana Uno," + DatosDeTest.emailUnico("a") + ",",
        "HUMANA,DNI," + docRoto + ",Roto Uno," + emailRoto + ",",
        "HUMANA,DNI," + docB + ",Bruno Dos," + DatosDeTest.emailUnico("b") + ","));

    assertEquals(2, resultado.getCreados());
    assertEquals(2, resultado.getCuentasCreadas());
    assertEquals(List.of(3), numerosDeFila(resultado));
    assertEquals(6, proveedor.creados.size(), "3 en el intento por tanda y 3 en el reintento.");
    assertEquals(4, proveedor.eliminados.size(), "Se compensan las 3 de la tanda y la de la fila rota.");
    assertTrue(repoDonantes.buscarPorDocumento(docA).isPresent());
    assertTrue(repoDonantes.buscarPorDocumento(docB).isPresent());
    assertFalse(repoPersonas.buscarPorDocumento(docRoto).isPresent());
  }

  @Test
  void losTotalesFinalesSonCorrectos() {
    String email = DatosDeTest.emailUnico("repetida");
    String documento = DatosDeTest.documentoUnico();
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor, csv(
        "HUMANA,DNI," + documento + ",Ana Perez," + email + ",1111111111",
        "HUMANA,DNI," + documento + ",Ana Perez Actualizada," + email + ",1199999999",
        "HUMANA,DNI",
        "JURIDICA,CUIT," + DatosDeTest.cuitUnico() + ",Empresa S.A.," + DatosDeTest.emailUnico("e") + ","));

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertNotNull(resultado.getFechaFin());
    assertEquals(4, resultado.getFilasLeidas());
    assertEquals(2, resultado.getCreados());
    assertEquals(1, resultado.getActualizados());
    assertEquals(2, resultado.getCuentasCreadas());
    assertEquals(0, resultado.getCuentasVinculadas());
    assertEquals(1, resultado.getFilasConError());
    assertEquals(List.of(4), numerosDeFila(resultado));
    assertEquals(0, resultado.getErroresNoGuardados());
  }

  @Test
  void conMilFilasTerminaConLosTotalesYLasLlamadasEsperadas() {
    List<String> filas = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      filas.add("HUMANA,DNI," + DatosDeTest.documentoUnico() + ",Persona Numerada,"
          + DatosDeTest.emailUnico("mil") + ",11" + (10_000_000 + i));
    }
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv resultado = importar(proveedor, csv(filas.toArray(String[]::new)));

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertEquals(1000, resultado.getFilasLeidas());
    assertEquals(1000, resultado.getCreados());
    assertEquals(1000, resultado.getCuentasCreadas());
    assertEquals(0, resultado.getFilasConError());
    assertEquals(1000, proveedor.creados.size());
    assertTrue(proveedor.eliminados.isEmpty());
  }

  @Test
  void siElArchivoNoSePuedeLeerQuedaFallidaConElMotivo() {
    LectorCsvDonantes lectorRoto = new LectorCsvDonantes() {
      @Override
      public Stream<LineaCsv> leer(Path archivo) {
        throw new ImportacionCsvException("No se pudo abrir el archivo CSV: " + archivo, null);
      }
    };
    ImportacionDonantesService service =
        nuevoService(new ProveedorFalso(), repoUsuarios, lectorRoto);

    ImportacionCsv resultado = importar(service, csv());

    assertEquals(EstadoImportacion.FALLIDA, resultado.getEstado());
    assertTrue(resultado.getMotivoFallo().contains("No se pudo abrir el archivo CSV"));
  }

  @Test
  void elArchivoDeEjemploChicoCreaCuatroYActualizaUno() {

    System.out.println("INICIO: ARCHIVO CHICO");

    ProveedorFalso proveedor = new ProveedorFalso();
    ImportacionDonantesService service = nuevoService(proveedor, repoUsuarios, new LectorCsvDonantes());

    long id = service.iniciar("donantes_prueba.csv", recurso("/donantes_prueba.csv"));
    ImportacionCsv resultado = service.buscar(id);

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertEquals(5, resultado.getFilasLeidas());
    assertEquals(4, resultado.getCreados());
    assertEquals(1, resultado.getActualizados());
    assertEquals(0, resultado.getFilasConError());
    assertEquals(4, proveedor.creados.size());

    Persona ana = repoPersonas.buscarPorDocumento("12345678").orElseThrow();
    assertEquals("Ana Perez Actualizada", ana.getNombreDisplay());
    assertEquals(List.of("1199999999"), telefonosDe(ana));
    Persona maria = repoPersonas.buscarPorDocumento("11111111").orElseThrow();
    assertFalse(maria.tieneContactoDeTipo(TipoContacto.TELEFONO));
    assertTrue(repoDonantes.buscarPorDocumento("30-12345678-9").isPresent());
  }

  @Test
  void elArchivoDeEjemploGrandeCreaTodosMenosLosEmailsRepetidos() {

    System.out.println("INICIO: ARCHIVO GRANDE");

    ProveedorFalso proveedor = new ProveedorFalso();
    ImportacionDonantesService service = nuevoService(proveedor, repoUsuarios, new LectorCsvDonantes());

    long id = service.iniciar("donantes_prueba_2.csv", recurso("/donantes_prueba_2.csv"));
    ImportacionCsv resultado = service.buscar(id);

    assertEquals(EstadoImportacion.FINALIZADA, resultado.getEstado());
    assertEquals(20000, resultado.getFilasLeidas());
    assertEquals(19986, resultado.getCreados());
    assertEquals(0, resultado.getActualizados());
    assertEquals(14, resultado.getFilasConError());
    assertTrue(resultado.getErrores().stream()
        .allMatch(error -> error.getMensaje().contains("ya está registrado para otro documento")));
    assertEquals(19986, proveedor.creados.size());

    PersonaHumana benjamin = (PersonaHumana) repoPersonas.buscarPorDocumento("11780798").orElseThrow();
    assertEquals("Benjamín", benjamin.getNombre());
    assertEquals("Castro", benjamin.getApellido());
    assertTrue(benjamin.tieneContacto(TipoContacto.EMAIL, "benjamíncastro8929@yahoo.com"));
  }

  @Test
  void importarDosVecesElMismoCsvNoDuplicaDonantesNiCreaCuentasLaSegundaVez() {
    String dni = DatosDeTest.documentoUnico();
    String cuit = DatosDeTest.cuitUnico();
    String emailHumana = DatosDeTest.emailUnico("dos-veces-h");
    String emailJuridica = DatosDeTest.emailUnico("dos-veces-j");
    String contenido = csv(
        "HUMANA,DNI," + dni + ",Ana Repetida," + emailHumana + ",",
        "JURIDICA,CUIT," + cuit + ",Empresa Repetida S.A.," + emailJuridica + ",");
    ProveedorFalso proveedor = new ProveedorFalso();

    ImportacionCsv primera = importar(proveedor, contenido);
    ImportacionCsv segunda = importar(proveedor, contenido);

    assertEquals(2, primera.getCreados());
    assertEquals(2, primera.getCuentasCreadas());
    assertEquals(0, segunda.getCreados());
    assertEquals(2, segunda.getActualizados());
    assertEquals(0, segunda.getCuentasCreadas());
    assertEquals(0, segunda.getCuentasVinculadas());
    assertEquals(0, segunda.getFilasConError());
    assertEquals(2, proveedor.creados.size(), "La segunda importacion no crea cuentas.");
    assertEquals(1, donantesConDocumento(dni));
    assertEquals(1, donantesConDocumento(cuit));
  }

  @Test
  void importarDespuesDeBorrarLaBaseVinculaLasCuentasExistentesSinCrearNinguna() {
    String dni = DatosDeTest.documentoUnico();
    String cuit = DatosDeTest.cuitUnico();
    String emailHumana = DatosDeTest.emailUnico("reset-h");
    String emailJuridica = DatosDeTest.emailUnico("reset-j");
    String contenido = csv(
        "HUMANA,DNI," + dni + ",Ana Reseteada," + emailHumana + ",",
        "JURIDICA,CUIT," + cuit + ",Empresa Reseteada S.A.," + emailJuridica + ",");
    ProveedorFalso proveedor = new ProveedorFalso();
    importar(proveedor, contenido);
    String idHumana = proveedor.idDe(emailHumana);
    String idJuridica = proveedor.idDe(emailJuridica);

    // Keycloak conserva las cuentas de la importacion anterior...
    proveedor.existentes.put(emailHumana, idHumana);
    proveedor.existentes.put(emailJuridica, idJuridica);
    // ...y la base queda vacia para esas personas, como despues de un reset.
    withTransaction(() -> {
      for (String documento : List.of(dni, cuit)) {
        Donante donante = repoDonantes.buscarPorDocumento(documento).orElseThrow();
        repoUsuarios.deDonante(donante).ifPresent(repoUsuarios::eliminar);
        repoDonantes.eliminar(donante);
        repoPersonas.eliminar(donante.getPersona());
      }
    });
    entityManager().clear();

    ImportacionCsv despues = importar(proveedor, contenido);

    assertEquals(EstadoImportacion.FINALIZADA, despues.getEstado());
    assertEquals(2, despues.getCreados());
    assertEquals(0, despues.getCuentasCreadas());
    assertEquals(2, despues.getCuentasVinculadas());
    assertEquals(2, proveedor.creados.size(), "No se crea ninguna cuenta nueva.");
    assertTrue(proveedor.eliminados.isEmpty());
    assertEquals(dni, repoUsuarios.buscarPorKeycloakId(idHumana).orElseThrow()
        .getDonante().getPersona().getDocumento());
    assertEquals(cuit, repoUsuarios.buscarPorKeycloakId(idJuridica).orElseThrow()
        .getDonante().getPersona().getDocumento());
  }

  @Test
  void siFallaUnaTandaLaCompensacionNoBorraUnaCuentaQueYaExistia() {
    String emailExistente = DatosDeTest.emailUnico("ya-existia");
    String idExistente = DatosDeTest.keycloakIdUnico();
    String emailRoto = DatosDeTest.emailUnico("roto");
    String docExistente = DatosDeTest.documentoUnico();
    ProveedorFalso proveedor = new ProveedorFalso();
    proveedor.existentes.put(emailExistente, idExistente);
    RepositorioUsuarios repoRoto = new RepositorioUsuarios() {
      @Override
      public void guardar(Usuario usuario) {
        if (emailRoto.equals(proveedor.emailDe(usuario.getKeycloakId()))) {
          throw new RuntimeException("DB caida simulada.");
        }
        super.guardar(usuario);
      }
    };

    importar(nuevoService(proveedor, repoRoto, new LectorCsvDonantes()), csv(
        "HUMANA,DNI," + DatosDeTest.documentoUnico() + ",Ana Nueva," + DatosDeTest.emailUnico("a") + ",",
        "HUMANA,DNI," + docExistente + ",Eva Existente," + emailExistente + ",",
        "HUMANA,DNI," + DatosDeTest.documentoUnico() + ",Roto Uno," + emailRoto + ","));

    assertFalse(proveedor.eliminados.contains(idExistente),
        "Solo se compensan las cuentas creadas por esta importacion.");
    assertEquals(docExistente, repoUsuarios.buscarPorKeycloakId(idExistente).orElseThrow()
        .getDonante().getPersona().getDocumento());
  }

  private long donantesConDocumento(String documento) {
    // Cuenta en la base, sin traer a todos los donantes.
    return entityManager().createQuery(
            "select count(d) from Donante d join d.persona p "
                + "left join PersonaHumana h on h.id = p.id "
                + "left join PersonaJuridica j on j.id = p.id "
                + "where h.documento = :documento or j.cuit = :documento",
            Long.class)
        .setParameter("documento", documento)
        .getSingleResult();
  }

  private ImportacionCsv importar(ProveedorFalso proveedor, String contenido) {
    return importar(nuevoService(proveedor, repoUsuarios, new LectorCsvDonantes()), contenido);
  }

  private ImportacionCsv importar(ImportacionDonantesService service, String contenido) {
    InputStream entrada = new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
    long id = service.iniciar(DatosDeTest.textoUnico("importacion") + ".csv", entrada);
    return service.buscar(id);
  }

  private ImportacionDonantesService nuevoService(ProveedorFalso proveedor,
                                                  RepositorioUsuarios usuarios,
                                                  LectorCsvDonantes lector) {
    return new ImportacionDonantesService(
        lector,
        new RepositorioImportaciones(),
        repoPersonas,
        repoDonantes,
        usuarios,
        new UsuariosService(proveedor, usuarios),
        Runnable::run,
        false);
  }

  private void donanteConCuenta(String documento, String email) {
    donanteConCuenta(documento, email, DatosDeTest.keycloakIdUnico());
  }

  private void donanteConCuenta(String documento, String email, String keycloakId) {
    PersonaHumana persona = new PersonaHumana("Ana", "Perez", 30, documento, Genero.FEMENINO);
    persona.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, email));
    repoPersonas.guardar(persona);
    Donante donante = new Donante(persona);
    repoDonantes.guardar(donante);
    repoUsuarios.guardar(Usuario.deDonante(keycloakId, donante));
  }

  private InputStream recurso(String nombre) {
    InputStream entrada = getClass().getResourceAsStream(nombre);
    assertNotNull(entrada, "No se encontro el recurso " + nombre);
    return entrada;
  }

  private static String csv(String... filas) {
    return ENCABEZADO + "\n" + String.join("\n", filas) + (filas.length == 0 ? "" : "\n");
  }

  private static List<String> telefonosDe(Persona persona) {
    return persona.getContactos().stream()
        .filter(c -> c.getTipo() == TipoContacto.TELEFONO)
        .map(MedioContacto::getValor)
        .toList();
  }

  private static List<Integer> numerosDeFila(ImportacionCsv importacion) {
    return importacion.getErrores().stream().map(ErrorDeImportacion::getNumeroFila).toList();
  }

  private static class ProveedorFalso implements ProveedorDeIdentidad {
    final List<String> creados = new ArrayList<>();
    final List<Boolean> conMail = new ArrayList<>();
    final List<String> busquedas = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();
    final Map<String, String> existentes = new HashMap<>();
    final Set<String> rechazados = new HashSet<>();
    private final Map<String, String> emailPorId = new HashMap<>();

    String idDe(String email) {
      return emailPorId.entrySet().stream()
          .filter(e -> e.getValue().equals(email))
          .map(Map.Entry::getKey)
          .reduce((primero, ultimo) -> ultimo)
          .orElseThrow();
    }

    String emailDe(String id) {
      return emailPorId.get(id);
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      return crearUsuarioSinContrasena(email, rol, true);
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol, boolean enviarMail) {
      if (existentes.containsKey(email)) {
        throw new ConflictoDeNegocioException("Ya existe una cuenta con ese correo.");
      }
      if (rechazados.contains(email)) {
        throw new IntegracionConIdentidadException(
            "Keycloak respondio 400 al crear el usuario: invalid email.");
      }
      creados.add(email);
      conMail.add(enviarMail);
      String id = DatosDeTest.keycloakIdUnico();
      emailPorId.put(id, email);
      return id;
    }

    @Override
    public Optional<String> buscarIdPorEmail(String email) {
      busquedas.add(email);
      return Optional.ofNullable(existentes.get(email));
    }

    @Override
    public void eliminarUsuario(String idExterno) {
      eliminados.add(idExterno);
    }
  }
}
