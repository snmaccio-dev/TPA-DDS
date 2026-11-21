package donatrack.donaciones;

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
import donatrack.donaciones.controller.AdministradoresController;
import donatrack.donaciones.controller.AsignacionesController;
import donatrack.donaciones.controller.DonacionesController;
import donatrack.donaciones.controller.DonantesController;
import donatrack.donaciones.controller.EntidadesBeneficiariasController;
import donatrack.donaciones.controller.ImportacionesController;
import donatrack.donaciones.controller.NecesidadesController;
import donatrack.donaciones.controller.NotificacionesController;
import donatrack.donaciones.controller.RegistroController;
import donatrack.donaciones.controller.SubcategoriasController;
import donatrack.donaciones.controller.UsuariosController;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.BandejaDeNotificacionesService;
import donatrack.donaciones.service.AsignacionesService;
import donatrack.donaciones.service.DonacionesService;
import donatrack.donaciones.service.DonantesService;
import donatrack.donaciones.service.EntidadesBeneficiariasService;
import donatrack.donaciones.service.ImportacionDonantesService;
import donatrack.donaciones.service.NecesidadesService;
import donatrack.donaciones.service.PersonasService;
import donatrack.donaciones.service.UbicacionesService;
import donatrack.donaciones.service.UsuariosService;
import io.javalin.Javalin;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Cubre que Rutas.registrarEn exponga correctamente todas las rutas:
//  - las publicas no piden token,
//  - las protegidas devuelven 401 sin token,
//  - /donantes/inactivos resuelve al handler propio y no cae en /donantes/{documento}.
class RutasTest {

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
  void registroDeDonantesNoPideTokenAunqueFalleElBody() {
    Dependencias deps = new Dependencias();
    Javalin app = app(deps);

    JavalinTest.test(app, (server, client) -> {
      var resp = client.post("/registro/donantes", "{}");
      assertNotEquals(401, resp.code(), "La ruta publica no debe pedir token.");
      assertNotEquals(403, resp.code(), "La ruta publica no debe exigir rol.");
    });
  }

  @Test
  void registroDeBeneficiariasNoPideTokenAunqueFalleElBody() {
    Dependencias deps = new Dependencias();
    Javalin app = app(deps);

    JavalinTest.test(app, (server, client) -> {
      var resp = client.post("/registro/beneficiarias", "{}");
      assertNotEquals(401, resp.code(), "La ruta publica no debe pedir token.");
      assertNotEquals(403, resp.code(), "La ruta publica no debe exigir rol.");
    });
  }

  @Test
  void rutasProtegidasSinTokenResponden401() {
    Dependencias deps = new Dependencias();
    Javalin app = app(deps);

    JavalinTest.test(app, (server, client) -> {
      assertEquals(401, client.get("/donaciones").code(),
          "GET /donaciones protegido: sin token debe dar 401.");
      assertEquals(401, client.get("/donantes").code(),
          "GET /donantes protegido: sin token debe dar 401.");
      assertEquals(401, client.post("/donantes/existente", "{}").code(),
          "POST /donantes/existente protegido: sin token debe dar 401.");
      assertEquals(401, client.get("/donantes/importaciones").code(),
          "GET /donantes/importaciones protegido: sin token debe dar 401.");
      assertEquals(401, client.put("/donaciones/1", "{}").code(),
          "PUT /donaciones/{id} protegido: sin token debe dar 401.");
      assertEquals(401, client.put("/donantes/12345678", "{}").code(),
          "PUT /donantes/{documento} protegido: sin token debe dar 401.");
      assertEquals(401, client.put("/entidades/1", "{}").code(),
          "PUT /entidades/{id} protegido: sin token debe dar 401.");
      assertEquals(401, client.get("/notificaciones/mias").code(),
          "GET /notificaciones/mias protegido: sin token debe dar 401.");
      assertEquals(401, client.post("/donaciones/eventos/entrega", "{}").code(),
          "POST /donaciones/eventos/entrega protegido: sin token debe dar 401.");
    });
  }

  @Test
  void lasUbicacionesDeEntidadesSonPublicasYNoCaenEnEntidadesPorId() {
    Dependencias deps = new Dependencias();
    when(deps.entidadesService.conUbicacion(List.of(1L, 2L))).thenReturn(List.of());
    Javalin app = app(deps);

    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/entidades/ubicaciones?ids=1,2");
      assertEquals(200, resp.code(), "GET /entidades/ubicaciones es publica.");
      assertEquals("[]", resp.body().string().trim());
    });

    org.mockito.Mockito.verify(deps.entidadesService).conUbicacion(List.of(1L, 2L));
    org.mockito.Mockito.verify(deps.entidadesService, org.mockito.Mockito.never())
        .buscar(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void laRutaPatchDeDireccionDeEntidadesYaNoExiste() {
    Dependencias deps = new Dependencias();
    Javalin app = app(deps);

    JavalinTest.test(app, (server, client) -> {
      assertEquals(404, client.patch("/entidades/1/direccion", "{}").code(),
          "La modificacion de entidades se hace con PUT /entidades/{id}.");
    });
  }

  @Test
  void donantesInactivosResuelveAlHandlerDeInactivos() throws Exception {
    Dependencias deps = new Dependencias();
    // Si Javalin confundiera "inactivos" con un documento, llegaria a buscarPorDocumento;
    // mockeamos inactivos() con una lista vacia y confirmamos que el 200 llega por aca.
    when(deps.donantesService.inactivos()).thenReturn(List.of());
    Javalin app = app(deps);

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app, (server, client) -> {
      var resp = client.get("/donantes/inactivos",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, resp.code(),
          "GET /donantes/inactivos tiene que caer en el handler de inactivos.");
      assertEquals("[]", resp.body().string().trim(),
          "El handler de inactivos devuelve la lista vacia mockeada.");
    });

    org.mockito.Mockito.verify(deps.donantesService).inactivos();
    org.mockito.Mockito.verify(deps.donantesService, org.mockito.Mockito.never())
        .buscarPorDocumento("inactivos");
  }

  // === Helpers ===

  // Arma una Javalin real con FiltroDeAcceso y todas las rutas, con los controllers
  // construidos sobre dependencias mockeadas.
  private Javalin app(Dependencias deps) {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, deps.repoUsuarios).registrarEn(app);

    new Rutas(
        new RegistroController(deps.donantesService, deps.entidadesService),
        new UsuariosController(),
        new AdministradoresController(deps.usuariosService),
        new DonacionesController(deps.donacionesService, deps.donantesService, deps.ubicacionesService),
        new AsignacionesController(deps.asignacionesService),
        new DonantesController(deps.donantesService),
        new ImportacionesController(deps.importacionService),
        new EntidadesBeneficiariasController(deps.entidadesService, deps.personasService),
        new NecesidadesController(deps.necesidadesService, deps.entidadesService),
        new SubcategoriasController(deps.donacionesService),
        new NotificacionesController(deps.bandejaDeNotificaciones)
    ).registrarEn(app);

    return app;
  }

  private static String firmar(String sub, String email, String... roles) throws Exception {
    List<String> listaRoles = new ArrayList<>();
    for (String r : roles) listaRoles.add(r);
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

  // Contenedor para no repetir los mocks en cada test.
  private static class Dependencias {
    final RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    final UsuariosService usuariosService = mock(UsuariosService.class);
    final DonacionesService donacionesService = mock(DonacionesService.class);
    final AsignacionesService asignacionesService = mock(AsignacionesService.class);
    final EntidadesBeneficiariasService entidadesService = mock(EntidadesBeneficiariasService.class);
    final NecesidadesService necesidadesService = mock(NecesidadesService.class);
    final PersonasService personasService = mock(PersonasService.class);
    final DonantesService donantesService = mock(DonantesService.class);
    final ImportacionDonantesService importacionService = mock(ImportacionDonantesService.class);
    final UbicacionesService ubicacionesService = mock(UbicacionesService.class);
    final BandejaDeNotificacionesService bandejaDeNotificaciones =
        mock(BandejaDeNotificacionesService.class);

    Dependencias() {
      when(repoUsuarios.buscarPorKeycloakId(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(Optional.empty());
    }
  }
}
