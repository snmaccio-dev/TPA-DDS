package donatrack.donaciones.seguridad;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorDeTokensTest {

  private static final String ISSUER = "https://kc.test/realms/donatrack";

  private static RSAKey clave;
  private static RSAKey otraClave;
  private static JWKSource<SecurityContext> fuente;
  private static VerificadorDeTokens verificador;

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-ok").generate();
    otraClave = new RSAKeyGenerator(2048).keyID("kid-otra").generate();
    fuente = new ImmutableJWKSet<>(new JWKSet(clave.toPublicJWK()));
    verificador = new VerificadorDeTokens(fuente, ISSUER);
  }

  @Test
  void unTokenValidoDevuelveSubEmailYRolesConocidos() throws Exception {
    String jwt = firmar(clave, ISSUER,
        Instant.now().plusSeconds(300),
        Map.of(
            "email", "ana@mail.com",
            "realm_access", Map.of("roles", List.of(
                "DONANTE", "offline_access", "default-roles-donatrack"))
        ));

    VerificadorDeTokens.TokenVerificado resultado =
        verificador.verificar("Bearer " + jwt);

    assertEquals("usuario-1", resultado.keycloakId());
    assertEquals("ana@mail.com", resultado.email());
    assertEquals(Set.of(RolDeAcceso.DONANTE), resultado.roles(),
        "Los roles desconocidos (offline_access, default-roles-...) se tienen que ignorar.");
  }

  @Test
  void unTokenVencidoDevuelve401() throws Exception {
    String jwt = firmar(clave, ISSUER,
        Instant.now().minusSeconds(60),
        Map.of("email", "x@mail.com", "realm_access", Map.of("roles", List.of("DONANTE"))));

    UnauthorizedResponse e = assertThrows(UnauthorizedResponse.class,
        () -> verificador.verificar("Bearer " + jwt));
    assertTrue(e.getMessage() != null);
  }

  @Test
  void unTokenConOtroIssuerDevuelve401() throws Exception {
    String jwt = firmar(clave, "https://otro.test/realms/donatrack",
        Instant.now().plusSeconds(300),
        Map.of("email", "x@mail.com", "realm_access", Map.of("roles", List.of("DONANTE"))));

    assertThrows(UnauthorizedResponse.class, () -> verificador.verificar("Bearer " + jwt));
  }

  @Test
  void unTokenFirmadoConOtraClaveDevuelve401() throws Exception {
    String jwt = firmar(otraClave, ISSUER,
        Instant.now().plusSeconds(300),
        Map.of("email", "x@mail.com", "realm_access", Map.of("roles", List.of("DONANTE"))));

    assertThrows(UnauthorizedResponse.class, () -> verificador.verificar("Bearer " + jwt));
  }

  @Test
  void unHeaderSinBearerDevuelve401() {
    assertThrows(UnauthorizedResponse.class, () -> verificador.verificar("Basic xxxx"));
  }

  @Test
  void unHeaderNullDevuelve401() {
    assertThrows(UnauthorizedResponse.class, () -> verificador.verificar(null));
  }

  private static String firmar(RSAKey clave, String issuer,
                               Instant expiracion, Map<String, Object> claimsExtra) throws Exception {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
        .subject("usuario-1")
        .issuer(issuer)
        .expirationTime(Date.from(expiracion));
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