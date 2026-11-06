package donatrack.logistica.seguridad;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.javalin.http.UnauthorizedResponse;

import java.net.URL;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VerificadorDeTokens {

  private static final String PREFIJO = "Bearer ";

  private final ConfigurableJWTProcessor<SecurityContext> procesador;

  public VerificadorDeTokens(String urlBaseKeycloak, String realm, String issuerEsperado) {
    if (urlBaseKeycloak == null || urlBaseKeycloak.isBlank()) {
      throw new IllegalArgumentException("La URL de Keycloak es obligatoria.");
    }
    if (realm == null || realm.isBlank()) {
      throw new IllegalArgumentException("El realm de Keycloak es obligatorio.");
    }

    String base = urlBaseKeycloak.endsWith("/")
        ? urlBaseKeycloak.substring(0, urlBaseKeycloak.length() - 1)
        : urlBaseKeycloak;

    String issuer = (issuerEsperado == null || issuerEsperado.isBlank())
        ? base + "/realms/" + realm
        : issuerEsperado;

    JWKSource<SecurityContext> jwkSource;
    try {
      URL jwks = new URL(base + "/realms/" + realm + "/protocol/openid-connect/certs");
      jwkSource = JWKSourceBuilder.create(jwks).build();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "No se pudo configurar el JWKS de Keycloak: " + e.getMessage(), e);
    }

    this.procesador = construirProcesador(jwkSource, issuer);
  }

  public VerificadorDeTokens(JWKSource<SecurityContext> jwkSource, String issuerEsperado) {
    if (issuerEsperado == null || issuerEsperado.isBlank()) {
      throw new IllegalArgumentException("El issuer esperado es obligatorio.");
    }

    this.procesador = construirProcesador(jwkSource, issuerEsperado);
  }

  private static ConfigurableJWTProcessor<SecurityContext> construirProcesador(
      JWKSource<SecurityContext> jwkSource, String issuer) {

    ConfigurableJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();

    procesador.setJWSKeySelector(
        new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));

    procesador.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
        new JWTClaimsSet.Builder().issuer(issuer).build(),
        new HashSet<>(List.of("sub", "exp", "iss"))
    ));

    return procesador;
  }

  public TokenVerificado verificar(String headerAutorizacion) {
    if (headerAutorizacion == null || !headerAutorizacion.startsWith(PREFIJO)) {
      throw new UnauthorizedResponse("Falta el header Authorization con prefijo Bearer.");
    }

    String raw = headerAutorizacion.substring(PREFIJO.length()).trim();

    if (raw.isEmpty()) {
      throw new UnauthorizedResponse("El token esta vacio.");
    }

    try {
      JWTClaimsSet claims = procesador.process(raw, null);
      Date vencimiento = claims.getExpirationTime();

      if (vencimiento == null || vencimiento.before(new Date())) {
        throw new UnauthorizedResponse("El token esta vencido.");
      }

      return new TokenVerificado(
          claims.getSubject(),
          claims.getStringClaim("email"),
          rolesDesde(claims)
      );
    } catch (UnauthorizedResponse e) {
      throw e;
    } catch (Exception e) {
      throw new UnauthorizedResponse("Token invalido: " + e.getMessage());
    }
  }

  private static Set<RolDeAcceso> rolesDesde(JWTClaimsSet claims) {
    Set<RolDeAcceso> resultado = EnumSet.noneOf(RolDeAcceso.class);

    try {
      Object realmAccess = claims.getClaim("realm_access");

      if (realmAccess instanceof Map<?, ?> mapa && mapa.get("roles") instanceof List<?> lista) {
        for (Object rol : lista) {
          if (rol != null) {
            RolDeAcceso.desdeNombre(rol.toString()).ifPresent(resultado::add);
          }
        }
      }
    } catch (Exception ignorado) {
      return resultado;
    }

    return resultado;
  }

  public record TokenVerificado(String keycloakId, String email, Set<RolDeAcceso> roles) {
  }
}
