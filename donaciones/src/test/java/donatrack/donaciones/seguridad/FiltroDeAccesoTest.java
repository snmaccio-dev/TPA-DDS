package donatrack.donaciones.seguridad;

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
import donatrack.donaciones.repository.RepositorioUsuarios;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiltroDeAccesoTest {

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
  void unaRutaSinRolesDeclaradosResponde403() {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);

    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    // Sin roles: el filtro la tiene que rechazar.
    app.get("/sin-roles", ctx -> ctx.result("no tendria que llegar aca"));

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/sin-roles");
      assertEquals(403, respuesta.code());
    });
  }

  @Test
  void unaRutaPublicaPasaSinToken() {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);

    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    app.get("/publico", ctx -> ctx.result("ok"), RolDeAcceso.PUBLICO);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/publico");
      assertEquals(200, respuesta.code());
      assertEquals("ok", respuesta.body().string());
    });
  }

  @Test
  void unaRutaProtegidaSinTokenDevuelve401() {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);

    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    app.get("/privado", ctx -> ctx.result("no tendria que llegar aca"), RolDeAcceso.ADMINISTRADOR);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/privado");
      assertEquals(401, respuesta.code());
    });
  }

  @Test
  void unRolQueNoCubreLaRutaDevuelve403() throws Exception {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId("usuario-1")).thenReturn(Optional.empty());
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);

    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    app.get("/solo-admin", ctx -> ctx.result("no tendria que llegar aca"), RolDeAcceso.ADMINISTRADOR);

    String jwt = firmar(Map.of(
        "email", "ana@mail.com",
        "realm_access", Map.of("roles", List.of("DONANTE"))));

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/solo-admin",
          req -> req.header("Authorization", "Bearer " + jwt));
      assertEquals(403, respuesta.code());
    });
  }

  @Test
  void elRolCorrectoLlegaAlHandlerYLeeElUsuarioActual() throws Exception {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId("usuario-1")).thenReturn(Optional.empty());
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);

    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    app.get("/privado", ctx -> {
      UsuarioActual usuario = FiltroDeAcceso.usuarioDe(ctx);
      assertNotNull(usuario);
      ctx.result(usuario.email());
    }, RolDeAcceso.ADMINISTRADOR);

    String jwt = firmar(Map.of(
        "email", "ana@mail.com",
        "realm_access", Map.of("roles", List.of("ADMINISTRADOR"))));

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/privado",
          req -> req.header("Authorization", "Bearer " + jwt));
      assertEquals(200, respuesta.code());
      assertEquals("ana@mail.com", respuesta.body().string());
    });
  }

  private static String firmar(Map<String, Object> claimsExtra) throws Exception {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
        .subject("usuario-1")
        .issuer(ISSUER)
        .expirationTime(Date.from(Instant.now().plusSeconds(300)));
    for (Map.Entry<String, Object> e : claimsExtra.entrySet()) {
      builder.claim(e.getKey(), e.getValue());
    }
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(clave.getKeyID())
            .build(),
        builder.build()
    );
    jwt.sign(new RSASSASigner(clave));
    return jwt.serialize();
  }
}