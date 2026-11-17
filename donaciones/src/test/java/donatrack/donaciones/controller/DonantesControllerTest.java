package donatrack.donaciones.controller;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.controller.dto.ErrorDTO;
import donatrack.donaciones.domain.DatosInvalidosException;
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
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.ImportacionDonantesService;
import donatrack.donaciones.service.NotificacionesService;
import donatrack.donaciones.service.RecursoInexistenteException;
import donatrack.donaciones.service.UsuariosService;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Usa el DonantesService real sobre la base de test: los datos se commitean
// en el setup para que los vea el hilo del servidor de JavalinTest.
class DonantesControllerTest implements WithSimplePersistenceUnit {

  private static final String ISSUER = "https://kc.test/realms/donatrack";
  private static RSAKey clave;
  private static VerificadorDeTokens verificador;

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioDonantes repoDonantes = new RepositorioDonantes();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-ok").generate();
    verificador = new VerificadorDeTokens(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(clave.toPublicJWK())), ISSUER);
  }

  @Test
  void getDonantesNoIncluyeAUnaPersonaQueEsSoloBeneficiaria() throws Exception {
    String documentoDonante = DatosDeTest.documentoUnico();
    String cuitBeneficiaria = DatosDeTest.cuitUnico();
    withTransaction(() -> {
      donantePersistido(documentoDonante);
      beneficiariaPersistida(cuitBeneficiaria);
    });

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(), (server, client) -> {
      var resp = client.get("/donantes",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      String body = resp.body().string();
      assertTrue(body.contains("\"" + documentoDonante + "\""),
          "El donante tiene que estar en el listado.");
      assertFalse(body.contains(cuitBeneficiaria),
          "Una persona que solo es beneficiaria no tiene que aparecer en /donantes.");
    });
  }

  @Test
  void getDonantePorDocumentoDeAlguienQueNoEsDonanteDa404() throws Exception {
    String cuitBeneficiaria = DatosDeTest.cuitUnico();
    withTransaction(() -> beneficiariaPersistida(cuitBeneficiaria));

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(), (server, client) -> {
      var resp = client.get("/donantes/" + cuitBeneficiaria,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(404, resp.code());
    });
  }

  @Test
  void getDonantePorDocumentoDeUnDonanteDevuelveSuPersona() throws Exception {
    String documentoDonante = DatosDeTest.documentoUnico();
    withTransaction(() -> donantePersistido(documentoDonante));

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(), (server, client) -> {
      var resp = client.get("/donantes/" + documentoDonante,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      assertTrue(resp.body().string().contains("\"" + documentoDonante + "\""));
    });
  }

  @Test
  void putDonanteDeAlguienQueNoEsDonanteDa404() throws Exception {
    String cuitBeneficiaria = DatosDeTest.cuitUnico();
    withTransaction(() -> beneficiariaPersistida(cuitBeneficiaria));

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(), (server, client) -> {
      var resp = client.put("/donantes/" + cuitBeneficiaria,
          "{\"razonSocial\":\"Otra\",\"contactoPredeterminado\":\"EMAIL\"}",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(404, resp.code());
    });

    entityManager().clear();
    assertFalse(repoPersonas.buscarJuridicaPorCuit(cuitBeneficiaria).orElseThrow()
        .getRazonSocial().equals("Otra"));
  }

  @Test
  void putDonanteDeUnDonanteDevuelveSuPersonaActualizada() throws Exception {
    String documentoDonante = DatosDeTest.documentoUnico();
    withTransaction(() -> donantePersistido(documentoDonante));

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(), (server, client) -> {
      var resp = client.put("/donantes/" + documentoDonante,
          "{\"nombre\":\"Ana\",\"apellido\":\"Renovada\",\"edad\":31,\"genero\":\"FEMENINO\","
              + "\"telefono\":\"+54 11 1111-1111\",\"contactoPredeterminado\":\"TELEFONO\","
              + "\"documento\":\"00000000\",\"email\":\"otro@mail.com\"}",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      String body = resp.body().string();
      assertTrue(body.contains("\"" + documentoDonante + "\""), body);
      assertTrue(body.contains("Ana Renovada"), body);
      assertTrue(body.contains("+54 11 1111-1111"), body);
    });
  }

  // === Helpers ===

  private Javalin app() {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());
    DonantesService service = new DonantesService(
        repoDonantes,
        repoPersonas,
        new RepositorioNotificaciones(),
        RepositorioDonaciones.getInstance(),
        repoEntidades,
        mock(NotificacionesService.class),
        mock(UsuariosService.class));

    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, repoUsuarios).registrarEn(app);
    donatrack.donaciones.Rutas.donantes(app, new DonantesController(service),
        new ImportacionesController(mock(ImportacionDonantesService.class)));
    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));
    return app;
  }

  // Espera una transaccion abierta.
  private void donantePersistido(String documento) {
    PersonaHumana persona = new PersonaHumana("Ana", "Donante", 30, documento, Genero.FEMENINO);
    repoPersonas.guardar(persona);
    repoDonantes.guardar(new Donante(persona));
  }

  // Espera una transaccion abierta.
  private void beneficiariaPersistida(String cuit) {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, DatosDeTest.textoUnico("Solo Benef"), TipoOrganizacion.ONG, "Rubro");
    repoPersonas.guardar(juridica);
    repoEntidades.guardar(new Beneficiaria(juridica));
  }

  private static String firmar(String sub, String email, String... roles) throws Exception {
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(clave.getKeyID())
            .build(),
        new JWTClaimsSet.Builder()
            .subject(sub)
            .issuer(ISSUER)
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("email", email)
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .build()
    );
    jwt.sign(new RSASSASigner(clave));
    return jwt.serialize();
  }
}
