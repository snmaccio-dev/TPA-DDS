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
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.ConsultaDeNotificaciones;
import donatrack.donaciones.service.RecursoInexistenteException;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacionesControllerTest {

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
  void unDonanteVeSoloLasSuyas() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    Donante donanteAna = donanteConId(10L, ana);
    Notificacion suya = Notificacion.paraDonante(donanteAna, TipoNotificacion.CAMBIO_ESTADO, "mi msj", null);

    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    when(consulta.propiasDe(
        argThat(u -> "kc-ana".equals(u.keycloakId())),
        anyBoolean(), anyInt(), anyInt())).thenReturn(List.of(suya));

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana", Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/notificaciones/mias",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      String body = resp.body().string();
      assertTrue(body.contains("mi msj"));
    });
  }

  @Test
  void unAdminVeLasDeAdministradores() throws Exception {
    Notificacion paraAdmins = Notificacion.paraAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA, "falla", null);

    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    when(consulta.propiasDe(
        argThat(u -> u.esAdministrador()),
        anyBoolean(), anyInt(), anyInt())).thenReturn(List.of(paraAdmins));

    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId("kc-admin")).thenReturn(Optional.empty());

    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/notificaciones/mias",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
      assertTrue(resp.body().string().contains("falla"));
    });
  }

  @Test
  void marcarLeidaDeOtroDaForbidden() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    PersonaHumana luis = personaConId(2L, "Luis");
    Donante donanteAna = donanteConId(10L, ana);
    Donante donanteLuis = donanteConId(20L, luis);
    Notificacion ajena = Notificacion.paraDonante(
        donanteLuis, TipoNotificacion.CAMBIO_ESTADO, "msj luis", null);

    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    when(consulta.buscarPorId(99L)).thenReturn(ajena);

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana", Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.patch("/notificaciones/99/leida", "",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });
  }

  @Test
  void unIdInexistenteDevuelve404() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    Donante donanteAna = donanteConId(10L, ana);

    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    when(consulta.buscarPorId(anyLong())).thenThrow(
        new RecursoInexistenteException("no existe"));

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana", Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.patch("/notificaciones/123/leida", "",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(404, resp.code());
    });
  }

  @Test
  void getTodasSinRolAdminDevuelve403() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    Donante donanteAna = donanteConId(10L, ana);
    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana", Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/notificaciones",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(403, resp.code());
    });
  }

  @Test
  void laPaginacionRespetaElMaximoAlLlamarAlServicio() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    Donante donanteAna = donanteConId(10L, ana);

    int[] tamanioObservado = new int[1];
    ConsultaDeNotificaciones consulta = mock(ConsultaDeNotificaciones.class);
    when(consulta.propiasDe(
        argThat(u -> u != null),
        anyBoolean(), anyInt(), anyInt())).thenAnswer(inv -> {
      tamanioObservado[0] = inv.getArgument(3);
      return List.of();
    });

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana", Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(consulta, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/notificaciones/mias?tamanio=500",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code());
    });

    assertEquals(100, tamanioObservado[0],
        "El controller tiene que recortar al maximo permitido.");
  }

  // === Helpers ===

  private Javalin app(ConsultaDeNotificaciones consulta, RepositorioUsuarios repoUsuarios) {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, repoUsuarios).registrarEn(app);
    new NotificacionesController(consulta).registrarRutas(app);

    app.exception(RecursoInexistenteException.class, (e, ctx) ->
        ctx.status(404).json(new ErrorDTO(e.getMessage())));
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));
    app.exception(ConflictoDeNegocioException.class, (e, ctx) ->
        ctx.status(409).json(new ErrorDTO(e.getMessage())));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).json(new ErrorDTO(e.getMessage())));
    return app;
  }

  private RepositorioUsuarios repoConUsuario(String keycloakId, Usuario usuario) {
    RepositorioUsuarios mock = mock(RepositorioUsuarios.class);
    when(mock.buscarPorKeycloakId(keycloakId)).thenReturn(Optional.of(usuario));
    return mock;
  }

  private static String firmar(String sub, String email, String... roles) throws Exception {
    List<String> listaRoles = new ArrayList<>();
    for (String r : roles) {
      listaRoles.add(r);
    }
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
            .claim("realm_access", Map.of("roles", listaRoles))
            .build()
    );
    jwt.sign(new RSASSASigner(clave));
    return jwt.serialize();
  }

  private PersonaHumana personaConId(long id, String nombre) throws Exception {
    PersonaHumana persona = new PersonaHumana(nombre, "Test", 30, "doc" + id, Genero.FEMENINO);
    setPrivateField(persona.getClass().getSuperclass(), persona, "id", id);
    return persona;
  }

  private Donante donanteConId(long id, PersonaHumana persona) throws Exception {
    Donante donante = new Donante(persona);
    setPrivateField(Donante.class, donante, "id", id);
    return donante;
  }

  @SuppressWarnings("unused")
  private Beneficiaria beneficiariaConId(long id, PersonaJuridica persona) throws Exception {
    Beneficiaria beneficiaria = new Beneficiaria(persona);
    setPrivateField(Beneficiaria.class, beneficiaria, "id", id);
    return beneficiaria;
  }

  @SuppressWarnings("unused")
  private PersonaJuridica personaJuridicaConId(long id, String cuit) throws Exception {
    PersonaJuridica juridica = new PersonaJuridica(
        cuit, "Test " + cuit, TipoOrganizacion.ONG, "rubro");
    setPrivateField(juridica.getClass().getSuperclass(), juridica, "id", id);
    return juridica;
  }

  private void setPrivateField(Class<?> clase, Object objeto, String nombre, Object valor)
      throws Exception {
    Field campo = clase.getDeclaredField(nombre);
    campo.setAccessible(true);
    campo.set(objeto, valor);
  }
}
