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
import donatrack.donaciones.domain.persona.Coordenadas;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntidadesBeneficiariasControllerTest {

  private static final String ISSUER = "https://kc.test/realms/donatrack";
  private static final String BODY =
      "{\"razonSocial\":\"Escuela Nueva\",\"tipoOrganizacion\":\"INSTITUCION\","
          + "\"rubro\":\"Educacion\",\"direccion\":\"Nueva 123\","
          + "\"telefono\":\"+54 11 5555-5555\",\"cuit\":\"30-99999999-9\","
          + "\"representantes\":[{\"nombre\":\"Rep\",\"apellido\":\"Uno\","
          + "\"email\":\"rep@escuela.com\"}]}";
  private static RSAKey clave;
  private static VerificadorDeTokens verificador;

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-ok").generate();
    verificador = new VerificadorDeTokens(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(clave.toPublicJWK())), ISSUER);
  }

  @Test
  void unAdministradorPuedeModificarCualquierEntidad() throws Exception {
    EntidadesBeneficiariasService service = serviceQueDevuelve(5L);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.put("/entidades/5", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      assertTrue(resp.body().string().contains("\"id\":5"));
    });

    verify(service).modificar(eq(5L), eq("Escuela Nueva"), eq("INSTITUCION"), eq("Educacion"),
        argThat(datos -> datos.direccion().equals("Nueva 123")
            && datos.telefono().equals("+54 11 5555-5555")
            && datos.representantes().size() == 1
            && datos.representantes().get(0).getEmail().equals("rep@escuela.com")));
  }

  @Test
  void unaBeneficiariaPuedeModificarSuPropiaEntidad() throws Exception {
    EntidadesBeneficiariasService service = serviceQueDevuelve(20L);
    RepositorioUsuarios repoUsuarios = repoConBeneficiaria("kc-escuela", 20L);

    String token = firmar("kc-escuela", "escuela@mail.com", "BENEFICIARIA");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.put("/entidades/20", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
    });

    verify(service).modificar(eq(20L), anyString(), anyString(), anyString(), any());
  }

  @Test
  void unaBeneficiariaNoPuedeModificarOtraEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = repoConBeneficiaria("kc-escuela", 20L);

    String token = firmar("kc-escuela", "escuela@mail.com", "BENEFICIARIA");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.put("/entidades/21", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });

    verify(service, never()).modificar(anyLong(), any(), any(), any(), any());
  }

  @Test
  void unDonanteNoPuedeModificarUnaEntidad() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.put("/entidades/20", BODY,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });

    verify(service, never()).modificar(anyLong(), any(), any(), any(), any());
  }

  @Test
  void sinRepresentantesDevuelve400() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.put("/entidades/5",
          "{\"razonSocial\":\"Escuela\",\"direccion\":\"Nueva 123\","
              + "\"telefono\":\"123\",\"representantes\":[]}",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(400, resp.code());
    });

    verify(service, never()).modificar(anyLong(), any(), any(), any(), any());
  }

  @Test
  void laRutaViejaDeDireccionYaNoExiste() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.patch("/entidades/5/direccion", "{\"direccion\":\"Nueva 123\"}",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(404, resp.code());
    });
  }

  @Test
  void lasUbicacionesSonPublicasYSoloTraenIdRazonSocialYCoordenadas() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    Beneficiaria ubicada = beneficiariaConId(5L);
    ubicada.ubicarEn(new Coordenadas(-34.6356, -58.3647));
    when(service.conUbicacion(List.of(5L, 7L))).thenReturn(List.of(ubicada));

    JavalinTest.test(app(service, mock(RepositorioUsuarios.class)), (server, client) -> {
      var resp = client.get("/entidades/ubicaciones?ids=5,7,5");
      assertEquals(200, resp.code());
      String cuerpo = resp.body().string();
      assertEquals("[{\"id\":5,\"razonSocial\":\"Escuela 5\","
          + "\"latitud\":-34.6356,\"longitud\":-58.3647}]", cuerpo);
    });
    verify(service).conUbicacion(List.of(5L, 7L));
  }

  @Test
  void unIdQueNoEsNumeroEnUbicacionesDa400() {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);

    JavalinTest.test(app(service, mock(RepositorioUsuarios.class)), (server, client) ->
        assertEquals(400, client.get("/entidades/ubicaciones?ids=5,abc").code()));
    verify(service, never()).conUbicacion(any());
  }

  @Test
  void elAltaDeEntidadAceptaCoordenadasOpcionales() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    when(service.registrarConPersonaNueva(any(), any(), any())).thenReturn(beneficiariaConId(9L));
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());
    String conCoordenadas = BODY.substring(0, BODY.length() - 1)
        + ",\"latitud\":-34.6098,\"longitud\":-58.3925}";

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.post("/entidades/nueva", conCoordenadas,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(201, resp.code());
    });
    verify(service).registrarConPersonaNueva(any(), any(),
        eq(new Coordenadas(-34.6098, -58.3925)));
  }

  @Test
  void elAltaConUnaSolaCoordenadaDa400() throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId(anyString())).thenReturn(Optional.empty());
    String conLatitudSola = BODY.substring(0, BODY.length() - 1) + ",\"latitud\":-34.6}";

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app(service, repoUsuarios), (server, client) -> {
      var resp = client.post("/entidades/nueva", conLatitudSola,
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(400, resp.code());
    });
    verify(service, never()).registrarConPersonaNueva(any(), any(), any());
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

  private EntidadesBeneficiariasService serviceQueDevuelve(long beneficiariaId) throws Exception {
    EntidadesBeneficiariasService service = mock(EntidadesBeneficiariasService.class);
    Beneficiaria beneficiaria = beneficiariaConId(beneficiariaId);
    when(service.modificar(anyLong(), any(), any(), any(), any())).thenReturn(beneficiaria);
    return service;
  }

  private Beneficiaria beneficiariaConId(long beneficiariaId) throws Exception {
    PersonaJuridica juridica = new PersonaJuridica(
        "30-" + beneficiariaId + "-7", "Escuela " + beneficiariaId,
        TipoOrganizacion.INSTITUCION, "Educacion");
    Beneficiaria beneficiaria = new Beneficiaria(juridica);
    Field campo = Beneficiaria.class.getDeclaredField("id");
    campo.setAccessible(true);
    campo.set(beneficiaria, beneficiariaId);
    return beneficiaria;
  }

  private RepositorioUsuarios repoConBeneficiaria(String keycloakId, long beneficiariaId)
      throws Exception {
    Beneficiaria beneficiaria = beneficiariaConId(beneficiariaId);

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
