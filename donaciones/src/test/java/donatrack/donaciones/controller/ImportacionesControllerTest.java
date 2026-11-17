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
import donatrack.donaciones.controller.dto.ErrorDTO;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.ImportacionDonantesService;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportacionesControllerTest {

  private static final String ISSUER = "https://kc.test/realms/donatrack";
  private static RSAKey clave;
  private static VerificadorDeTokens verificador;

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-ok").generate();
    verificador = new VerificadorDeTokens(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(clave.toPublicJWK())), ISSUER);
  }

  @Test
  void sinArchivoDevuelve400() throws Exception {
    ImportacionDonantesService service = mock(ImportacionDonantesService.class);
    String token = firmar("kc-admin", "ADMINISTRADOR");

    JavalinTest.test(app(service), (server, client) -> {
      var resp = client.post("/donantes/importaciones", "",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(400, resp.code());
    });

    verify(service, never()).iniciar(anyString(), any());
  }

  @Test
  void unArchivoQueNoEsCsvDevuelve400() throws Exception {
    ImportacionDonantesService service = mock(ImportacionDonantesService.class);
    String token = firmar("kc-admin", "ADMINISTRADOR");

    JavalinTest.test(app(service), (server, client) -> {
      var resp = client.request("/donantes/importaciones", req -> req
          .header("Authorization", "Bearer " + token)
          .post(multipart("donantes.txt", "hola")));
      assertEquals(400, resp.code());
    });

    verify(service, never()).iniciar(anyString(), any());
  }

  @Test
  void conUnCsvDevuelve202ConElId() throws Exception {
    ImportacionDonantesService service = mock(ImportacionDonantesService.class);
    when(service.iniciar(eq("donantes.csv"), any(InputStream.class))).thenReturn(42L);
    String token = firmar("kc-admin", "ADMINISTRADOR");

    JavalinTest.test(app(service), (server, client) -> {
      var resp = client.request("/donantes/importaciones", req -> req
          .header("Authorization", "Bearer " + token)
          .post(multipart("donantes.csv",
              "TipoPersona,TipoDoc,Documento,NombreRazonSocial,Email,Telefono\n")));
      assertEquals(202, resp.code());
      assertTrue(resp.body().string().contains("\"importacionId\":42"));
    });
  }

  @Test
  void unaImportacionInexistenteDevuelve404() throws Exception {
    ImportacionDonantesService service = mock(ImportacionDonantesService.class);
    when(service.buscar(999L)).thenThrow(
        new RecursoInexistenteException("No existe la importacion con id 999"));
    String token = firmar("kc-admin", "ADMINISTRADOR");

    JavalinTest.test(app(service), (server, client) -> {
      var resp = client.get("/donantes/importaciones/999",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(404, resp.code());
    });
  }

  @Test
  void sinRolAdministradorDevuelve403() throws Exception {
    ImportacionDonantesService service = mock(ImportacionDonantesService.class);
    String token = firmar("kc-ana", "DONANTE");

    JavalinTest.test(app(service), (server, client) -> {
      var post = client.request("/donantes/importaciones", req -> req
          .header("Authorization", "Bearer " + token)
          .post(multipart("donantes.csv", "x\n")));
      assertEquals(403, post.code());
      var get = client.get("/donantes/importaciones",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, get.code());
    });

    verify(service, never()).iniciar(anyString(), any());
  }

  private Javalin app(ImportacionDonantesService service) {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, repoUsuarios).registrarEn(app);
    donatrack.donaciones.Rutas.donantes(app,
        new DonantesController(mock(DonantesService.class)),
        new ImportacionesController(service));
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));
    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));
    return app;
  }

  private static RequestBody multipart(String nombreArchivo, String contenido) {
    return new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("archivo", nombreArchivo, RequestBody.create(
            contenido.getBytes(StandardCharsets.UTF_8), MediaType.parse("text/csv")))
        .build();
  }

  private static String firmar(String sub, String... roles) throws Exception {
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(clave.getKeyID())
            .build(),
        new JWTClaimsSet.Builder()
            .subject(sub)
            .issuer(ISSUER)
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("email", sub + "@mail.com")
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .build()
    );
    jwt.sign(new RSASSASigner(clave));
    return jwt.serialize();
  }
}
