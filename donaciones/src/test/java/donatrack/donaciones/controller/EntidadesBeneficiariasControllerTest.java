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
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import donatrack.donaciones.service.PersonasService;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntidadesBeneficiariasControllerTest {

  private static final String ISSUER = "https://kc.test/realms/donatrack";
  private static final String BODY = "{\"direccion\":\"Nueva 123\"}";
  private static RSAKey clave;
  private static VerificadorDeTokens verificador;

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-ok").generate();
    verificador = new VerificadorDeTokens(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(clave.toPublicJWK())), ISSUER);
  }

  @Test
  void unAdministradorPuedeActualizarLaDireccionDeCualquierEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.patch("/entidades/5/direccion", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(204, resp.code());
    });

    verify(service).actualizarDireccion(5L, "Nueva 123");
  }

  @Test
  void unaBeneficiariaPuedeActualizarLaDireccionDeSuPropiaEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = repoConBeneficiaria("kc-escuela", 20L);

    String token = firmar("kc-escuela", "escuela@mail.com", "BENEFICIARIA");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.patch("/entidades/20/direccion", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(204, resp.code());
    });

    verify(service).actualizarDireccion(20L, "Nueva 123");
  }

  @Test
  void unaBeneficiariaNoPuedeActualizarLaDireccionDeOtraEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = repoConBeneficiaria("kc-escuela", 20L);

    String token = firmar("kc-escuela", "escuela@mail.com", "BENEFICIARIA");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.patch("/entidades/21/direccion", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });

    verify(service, never()).actualizarDireccion(anyLong(), anyString());
  }

  @Test
  void unDonanteNoPuedeActualizarLaDireccionDeUnaEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.patch("/entidades/20/direccion", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });

    verify(service, never()).actualizarDireccion(anyLong(), anyString());
  }

  // === Helpers ===

  private Javalin app(EntidadesBeneficiariasService service, RepositorioUsuarios repoUsuarios) {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, repoUsuarios).registrarEn(app);
    donatrack.donaciones.Rutas.entidades(app,
        new EntidadesBeneficiariasController(service, mock(PersonasService.class)));
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));
    return app;
  }

  private RepositorioUsuarios repoConBeneficiaria(String keycloakId, long beneficiariaId)
      throws Exception {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-" + beneficiariaId + "-7", "Escuela " + beneficiariaId,
        TipoOrganizacion.INSTITUCION, "Educacion");
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    Field campo = Beneficiaria.class.getDeclaredField("id");
    campo.setAccessible(true);
    campo.set(beneficiaria, beneficiariaId);

    RepositorioUsuarios repo = mock(RepositorioUsuarios.class);
    when(repo.buscarPorKeycloakId(keycloakId))
        .thenReturn(Optional.of(Usuario.deBeneficiaria(keycloakId, beneficiaria)));
    return repo;
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
