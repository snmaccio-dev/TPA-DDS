package donatrack.logistica.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
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
import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.EventoRutasPlanificadas;
import donatrack.logistica.contrato.PerfilDeUsuario;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.repository.RepositorioEntregas;
import donatrack.logistica.seguridad.ControlDeAccesoPorToken;
import donatrack.logistica.seguridad.FiltroDeAcceso;
import donatrack.logistica.seguridad.VerificadorDeTokens;
import donatrack.logistica.service.GestorEntregas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// GET /entregas/mias: la beneficiaria sale del token y del perfil que devuelve Donaciones,
// nunca de un parametro que mande el cliente. Corre contra Javalin real y los handlers
// usan otro hilo (otro EntityManager), asi que los datos se confirman en la base y se
// borran al terminar cada test.
class EntregasMiasTest implements WithSimplePersistenceUnit {

  private static final String ISSUER = "https://kc.test/realms/donatrack";
  private static final String CUENTA_A = "cuenta-beneficiaria-a";
  private static final long BENEFICIARIA_A = 9401L;
  private static final long BENEFICIARIA_B = 9402L;

  private static RSAKey clave;
  private static VerificadorDeTokens verificador;

  private final ObjectMapper json = new ObjectMapper();
  private final RepositorioEntregas repositorio = RepositorioEntregas.getInstance();
  private final List<Long> creadas = new ArrayList<>();

  @BeforeAll
  static void generarClaves() throws Exception {
    clave = new RSAKeyGenerator(2048).keyID("kid-mias").generate();
    verificador = new VerificadorDeTokens(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(clave.toPublicJWK())), ISSUER);
  }

  @BeforeEach
  void cargarEntregas() {
    withTransaction(() -> {
      guardar(new Entrega(9501L, 500L, BENEFICIARIA_A, "Destino A", "Leche", 10, "litros"));
      guardar(new Entrega(9502L, 500L, BENEFICIARIA_A, "Destino A", "Arroz", 5, "kg"));
      guardar(new Entrega(9503L, 500L, BENEFICIARIA_B, "Destino B", "Ropa", 2, "bolsas"));
    });
  }

  @AfterEach
  void borrarEntregas() {
    withTransaction(() -> creadas.forEach(repositorio::eliminar));
    WithSimplePersistenceUnit.dispose();
  }

  @Test
  void laBeneficiariaSoloVeSusEntregas() throws Exception {
    String token = firmar(CUENTA_A, "BENEFICIARIA");

    JavalinTest.test(app(), (server, client) -> {
      var respuesta = client.get("/entregas/mias",
          req -> req.header("Authorization", "Bearer " + token));

      assertEquals(200, respuesta.code());
      List<Map<String, Object>> entregas = leer(respuesta.body().string());
      assertEquals(2, entregas.size());
      assertTrue(entregas.stream().allMatch(e -> beneficiariaDe(e) == BENEFICIARIA_A));
    });
  }

  @Test
  void unParametroDelClienteNoCambiaLaBeneficiaria() throws Exception {
    String token = firmar(CUENTA_A, "BENEFICIARIA");

    JavalinTest.test(app(), (server, client) -> {
      var respuesta = client.get("/entregas/mias?beneficiaria=" + BENEFICIARIA_B,
          req -> req.header("Authorization", "Bearer " + token));

      assertEquals(200, respuesta.code());
      List<Map<String, Object>> entregas = leer(respuesta.body().string());
      assertEquals(2, entregas.size());
      assertTrue(entregas.stream().noneMatch(e -> beneficiariaDe(e) == BENEFICIARIA_B));
    });
  }

  @Test
  void soloConfirmablesDejaAfueraLasQueNoEstanEnTraslado() throws Exception {
    String token = firmar(CUENTA_A, "BENEFICIARIA");

    JavalinTest.test(app(), (server, client) -> {
      var respuesta = client.get("/entregas/mias?soloConfirmables=true",
          req -> req.header("Authorization", "Bearer " + token));

      assertEquals(200, respuesta.code());
      assertTrue(leer(respuesta.body().string()).isEmpty());
    });
  }

  @Test
  void unaCuentaSinPerfilDeBeneficiariaRecibe403() throws Exception {
    String token = firmar("cuenta-sin-perfil", "BENEFICIARIA");

    JavalinTest.test(app(), (server, client) -> {
      var respuesta = client.get("/entregas/mias",
          req -> req.header("Authorization", "Bearer " + token));

      assertEquals(403, respuesta.code());
    });
  }

  @Test
  void unDonanteNoPuedeUsarElEndpoint() throws Exception {
    String token = firmar(CUENTA_A, "DONANTE");

    JavalinTest.test(app(), (server, client) -> {
      var respuesta = client.get("/entregas/mias",
          req -> req.header("Authorization", "Bearer " + token));

      assertEquals(403, respuesta.code());
    });
  }

  private Javalin app() {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador).registrarEn(app);
    app.after(ctx -> WithSimplePersistenceUnit.dispose());

    GestorEntregas gestor = new GestorEntregas(
        new DonacionesQueNoHaceNada(), "http://monitoreo.test", foto -> foto.nombre());
    new EntregasController(gestor, new ControlDeAccesoPorToken(EntregasMiasTest::perfilDe))
        .registrarRutas(app);

    return app;
  }

  // Simula GET /usuarios/yo de Donaciones: la cuenta A es la beneficiaria A; cualquier
  // otra cuenta no tiene perfil de entidad beneficiaria.
  private static PerfilDeUsuario perfilDe(String autorizacion) {
    String sujeto;
    try {
      sujeto = SignedJWT.parse(autorizacion.substring("Bearer ".length()))
          .getJWTClaimsSet()
          .getSubject();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    return CUENTA_A.equals(sujeto)
        ? new PerfilDeUsuario(sujeto, "a@mail.com", null, BENEFICIARIA_A, "Entidad A")
        : new PerfilDeUsuario(sujeto, "x@mail.com", null, null, "Sin perfil");
  }

  private void guardar(Entrega entrega) {
    repositorio.guardar(entrega);
    entityManager().flush();
    creadas.add(entrega.getId());
  }

  private List<Map<String, Object>> leer(String cuerpo) throws Exception {
    return json.readValue(cuerpo, new TypeReference<List<Map<String, Object>>>() {});
  }

  private static long beneficiariaDe(Map<String, Object> entrega) {
    return ((Number) entrega.get("beneficiariaId")).longValue();
  }

  private static String firmar(String sujeto, String rol) throws Exception {
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(sujeto)
        .issuer(ISSUER)
        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
        .claim("email", sujeto + "@mail.com")
        .claim("realm_access", Map.of("roles", List.of(rol)))
        .build();
    SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(clave.getKeyID())
            .build(),
        claims
    );
    jwt.sign(new RSASSASigner(clave));
    return jwt.serialize();
  }

  private static class DonacionesQueNoHaceNada implements ClienteDonaciones {

    @Override
    public List<DonacionParaPlanificar> donacionesConAsignacionRealizada() {
      return List.of();
    }

    @Override
    public void publicarRutasPlanificadas(EventoRutasPlanificadas evento) {
    }

    @Override
    public void publicarInicioRuta(EventoInicioRuta evento) {
    }

    @Override
    public void publicarEntrega(EventoEntrega evento) {
    }

    @Override
    public void publicarRetornoADeposito(EventoRetornoDeposito evento) {
    }
  }
}
