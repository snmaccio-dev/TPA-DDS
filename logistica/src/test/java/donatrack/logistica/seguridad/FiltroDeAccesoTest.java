package donatrack.logistica.seguridad;

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
import donatrack.logistica.seguridad.web.TokensDeKeycloak;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  void unTokenValidoEnLaSesionDejaEntrarALaPantallaDeCamiones() throws Exception {
    String jwt = firmar(Map.of(
        "email", "admin@mail.com",
        "realm_access", Map.of("roles", List.of("ADMINISTRADOR"))));

    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador).registrarEn(app);
    // Simula lo que deja el callback de login en la sesion.
    app.get("/preparar-sesion", ctx -> ctx.sessionAttribute(
        TokensDeKeycloak.CLAVE_SESION, new TokensDeKeycloak(jwt, null, null, 300)),
        RolDeAcceso.PUBLICO);
    app.get("/admin/camiones",
        ctx -> ctx.result(FiltroDeAcceso.usuarioDe(ctx).email()),
        RolDeAcceso.ADMINISTRADOR);

    JavalinTest.test(app, (server, client) -> {
      String setCookie = client.get("/preparar-sesion").header("Set-Cookie");
      assertNotNull(setCookie, "La primera respuesta tendria que crear la sesion.");
      String cookie = setCookie.split(";")[0];

      var respuesta = client.get("/admin/camiones", req -> req.header("Cookie", cookie));

      assertEquals(200, respuesta.code());
      assertEquals("admin@mail.com", respuesta.body().string());
    });
  }

  @Test
  void sinTokenLaPantallaDeCamionesRedirigeAlIngreso() {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador).registrarEn(app);
    app.get("/admin/camiones/ingresar", ctx -> ctx.result("ingreso"), RolDeAcceso.PUBLICO);
    app.get("/admin/camiones", ctx -> ctx.result("no tendria que llegar aca"),
        RolDeAcceso.ADMINISTRADOR);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/admin/camiones");

      // El cliente de prueba sigue el redirect: la respuesta final es la del ingreso.
      assertNotNull(respuesta.priorResponse());
      assertEquals(302, respuesta.priorResponse().code());
      assertEquals("/admin/camiones/ingresar", respuesta.request().url().encodedPath());
      assertEquals("ingreso", respuesta.body().string());
    });
  }

  @Test
  void laApiSinHeaderSigueDevolviendo401() {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador).registrarEn(app);
    app.get("/camiones", ctx -> ctx.result("no tendria que llegar aca"),
        RolDeAcceso.ADMINISTRADOR);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/camiones");

      assertEquals(401, respuesta.code());
      assertNull(respuesta.priorResponse(), "La API no tiene que redirigir al login.");
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
