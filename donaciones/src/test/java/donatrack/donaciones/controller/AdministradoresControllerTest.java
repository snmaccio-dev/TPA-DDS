package donatrack.donaciones.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import donatrack.donaciones.controller.dto.NuevoAdministradorRequest;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.ProveedorDeIdentidad;
import donatrack.donaciones.seguridad.RolDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdministradoresControllerTest {

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
  void unAltaValidaResponde201ConElKeycloakIdYLlamaACrearUsuario() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-nuevo-admin");
    Javalin app = app(proveedor);
    ObjectMapper json = new ObjectMapper();

    JavalinTest.test(app, (server, client) -> {
      String body = "{\"email\":\"admin@mail.com\",\"contrasena\":\"unaLarga123\"}";
      var respuesta = client.post("/administradores", body);

      assertEquals(201, respuesta.code());
      String cuerpo = respuesta.body().string();
      assertTrue(cuerpo.contains("kc-nuevo-admin"), "El 201 tiene que devolver el keycloakId.");

      assertEquals(1, proveedor.creados.size(), "Hay que llamar a crearUsuario exactamente una vez.");
      LlamadaCreacion llamada = proveedor.creados.get(0);
      assertEquals("admin@mail.com", llamada.email);
      assertEquals("unaLarga123", llamada.contrasena);
      assertEquals(RolDeAcceso.ADMINISTRADOR, llamada.rol);
      assertTrue(proveedor.llamadasSinContrasena.isEmpty(),
          "No tiene que llamar a crearUsuarioSinContrasena.");
    });
  }

  @Test
  void sinEmailResponde400YNoLlamaAlProveedor() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-se-usa");
    Javalin app = app(proveedor);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.post("/administradores",
          "{\"email\":\"\",\"contrasena\":\"unaLarga123\"}");
      assertEquals(400, respuesta.code());
      assertTrue(proveedor.creados.isEmpty());
    });
  }

  @Test
  void contrasenaCortaResponde400YNoLlamaAlProveedor() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-se-usa");
    Javalin app = app(proveedor);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.post("/administradores",
          "{\"email\":\"admin@mail.com\",\"contrasena\":\"1234\"}");
      assertEquals(400, respuesta.code());
      assertTrue(proveedor.creados.isEmpty());
    });
  }

  @Test
  void contrasenaVaciaResponde400YNoLlamaAlProveedor() {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-se-usa");
    Javalin app = app(proveedor);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.post("/administradores",
          "{\"email\":\"admin@mail.com\",\"contrasena\":\"\"}");
      assertEquals(400, respuesta.code());
      assertTrue(proveedor.creados.isEmpty());
    });
  }

  @Test
  void siElProveedorLanzaConflictoResponde409() {
    ProveedorDeIdentidadFalso proveedor = ProveedorDeIdentidadFalso.conConflicto();
    Javalin app = app(proveedor);

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.post("/administradores",
          "{\"email\":\"admin@mail.com\",\"contrasena\":\"unaLarga123\"}");
      assertEquals(409, respuesta.code());
    });
  }

  @Test
  void elToStringDelRequestNoContieneLaContrasena() {
    NuevoAdministradorRequest request =
        new NuevoAdministradorRequest("admin@mail.com", "SuperSecreta123!");
    String s = request.toString();
    assertFalse(s.contains("SuperSecreta123!"),
        "toString no debe exponer la contrasena.");
    assertTrue(s.contains("admin@mail.com"));
    assertTrue(s.contains("***"));
  }

  @Test
  void conFiltroDeAccesoUnJwtSinRolAdministradorDevuelve403() throws Exception {
    ProveedorDeIdentidadFalso proveedor = new ProveedorDeIdentidadFalso("kc-no-se-usa");
    Javalin app = appConFiltro(proveedor);

    String jwt = firmar(Map.of(
        "email", "ana@mail.com",
        "realm_access", Map.of("roles", List.of("DONANTE"))));

    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.post("/administradores",
          "{\"email\":\"admin@mail.com\",\"contrasena\":\"unaLarga123\"}",
          req -> req.header("Authorization", "Bearer " + jwt));
      assertEquals(403, respuesta.code());
      assertTrue(proveedor.creados.isEmpty());
    });
  }

  // === Helpers ===

  // App minima sin FiltroDeAcceso para probar la logica del controller.
  // Replica el manejo de excepciones de DonacionesApp para los dos codigos
  // que usa este endpoint (400 y 409).
  private Javalin app(ProveedorDeIdentidad proveedor) {
    Javalin app = Javalin.create();
    new AdministradoresController(proveedor).registrarRutas(app);
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).result(e.getMessage()));
    app.exception(ConflictoDeNegocioException.class, (e, ctx) ->
        ctx.status(409).result(e.getMessage()));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).result(e.getMessage()));
    return app;
  }

  // Igual que app(), pero sumando el FiltroDeAcceso con un verificador real
  // para poder probar el 403 cuando el JWT no trae ADMINISTRADOR.
  private Javalin appConFiltro(ProveedorDeIdentidad proveedor) {
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId("usuario-1")).thenReturn(Optional.empty());
    FiltroDeAcceso filtro = new FiltroDeAcceso(verificador, repoUsuarios);
    Javalin app = Javalin.create();
    filtro.registrarEn(app);
    new AdministradoresController(proveedor).registrarRutas(app);
    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).result(e.getMessage()));
    app.exception(ConflictoDeNegocioException.class, (e, ctx) ->
        ctx.status(409).result(e.getMessage()));
    app.exception(HttpResponseException.class, (e, ctx) ->
        ctx.status(e.getStatus()).result(e.getMessage()));
    return app;
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

  // === Fake que registra las llamadas que recibe ===

  private static class ProveedorDeIdentidadFalso implements ProveedorDeIdentidad {
    final List<LlamadaCreacion> creados = new ArrayList<>();
    final List<String> llamadasSinContrasena = new ArrayList<>();
    final List<String> eliminados = new ArrayList<>();
    private final String idFijo;
    private final boolean conflicto;

    ProveedorDeIdentidadFalso(String idFijo) {
      this.idFijo = idFijo;
      this.conflicto = false;
    }

    private ProveedorDeIdentidadFalso(boolean conflicto) {
      this.idFijo = null;
      this.conflicto = conflicto;
    }

    static ProveedorDeIdentidadFalso conConflicto() {
      return new ProveedorDeIdentidadFalso(true);
    }

    @Override
    public String crearUsuario(String email, String contrasena, RolDeAcceso rol) {
      if (conflicto) {
        throw new ConflictoDeNegocioException("Ya existe una cuenta con ese correo.");
      }
      creados.add(new LlamadaCreacion(email, contrasena, rol));
      return idFijo;
    }

    @Override
    public String crearUsuarioSinContrasena(String email, RolDeAcceso rol) {
      llamadasSinContrasena.add(email);
      return idFijo;
    }

    @Override
    public void eliminarUsuario(String idExterno) {
      eliminados.add(idExterno);
    }
  }

  private record LlamadaCreacion(String email, String contrasena, RolDeAcceso rol) {}
}