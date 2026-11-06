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
import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.estado.EstadosPosiblesDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.domain.usuario.Usuario;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioSubcategorias;
import donatrack.donaciones.repository.RepositorioUsuarios;
import donatrack.donaciones.seguridad.FiltroDeAcceso;
import donatrack.donaciones.seguridad.VerificadorDeTokens;
import donatrack.donaciones.service.GestorDonaciones;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DonacionesControllerTest {

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
  void unDonanteConSubcategoriaRecibeSoloLasSuyasDeEsaSubcategoria() throws Exception {
    PersonaHumana ana = personaConId(1L, "Ana");
    Donante donanteAna = donanteConId(10L, ana);
    Donacion sillaDeAna = donacionDe(donanteAna, "Sillas", "Mobiliario", 100L);

    GestorDonaciones gestor = mock(GestorDonaciones.class);
    when(gestor.buscar(
        argThat(filtro -> filtro != null
            && filtro.estado() == null
            && filtro.categoria() == null
            && filtro.subcategorias() != null
            && filtro.subcategorias().equals(List.of("Sillas"))),
        argThat(donante -> donante != null && Long.valueOf(10L).equals(donante.getId())),
        isNull()
    )).thenReturn(List.of(sillaDeAna));

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-ana",
        Usuario.deDonante("kc-ana", donanteAna));
    Javalin app = app(gestor, repoUsuarios);

    String token = firmar("kc-ana", "ana@mail.com", "DONANTE");
    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/donaciones?subcategoria=Sillas",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, respuesta.code());
      String body = respuesta.body().string();
      assertTrue(body.contains("Sillas"),
          "La respuesta debe incluir la subcategoria de la donacion del donante.");
      assertTrue(body.contains("Mobiliario"),
          "La respuesta debe incluir el nombre de la categoria.");
    });
  }

  @Test
  void unaBeneficiariaConEstadoRecibeSoloLasAsignadasEnEseEstado() throws Exception {
    PersonaJuridica escuelaPersona = personaJuridicaConId(2L);
    Beneficiaria escuela = beneficiariaConId(20L, escuelaPersona);
    PersonaHumana luis = personaConId(3L, "Luis");
    Donante donanteLuis = donanteConId(30L, luis);
    Donacion asignada = donacionDe(donanteLuis, "Mesas", "Mobiliario", 200L);

    GestorDonaciones gestor = mock(GestorDonaciones.class);
    when(gestor.buscar(
        argThat(filtro -> filtro != null
            && filtro.estado() == EstadosPosiblesDonacion.ASIGNACION_REALIZADA
            && filtro.categoria() == null
            && filtro.subcategorias() == null),
        isNull(),
        argThat(beneficiaria -> beneficiaria != null
            && Long.valueOf(20L).equals(beneficiaria.getId()))
    )).thenReturn(List.of(asignada));

    RepositorioUsuarios repoUsuarios = repoConUsuario("kc-escuela",
        Usuario.deBeneficiaria("kc-escuela", escuela));
    Javalin app = app(gestor, repoUsuarios);

    String token = firmar("kc-escuela", "escuela@mail.com", "BENEFICIARIA");
    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/donaciones?estado=ASIGNACION_REALIZADA",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(200, respuesta.code());
      String body = respuesta.body().string();
      assertTrue(body.contains("Mesas"),
          "La respuesta debe tener la donacion asignada a la beneficiaria.");
    });
  }

  @Test
  void unEstadoInvalidoDevuelve400() throws Exception {
    GestorDonaciones gestor = mock(GestorDonaciones.class);
    RepositorioUsuarios repoUsuarios = mock(RepositorioUsuarios.class);
    when(repoUsuarios.buscarPorKeycloakId("kc-admin")).thenReturn(Optional.empty());
    Javalin app = app(gestor, repoUsuarios);

    String token = firmar("kc-admin", "admin@mail.com", "ADMINISTRADOR");
    JavalinTest.test(app, (server, client) -> {
      var respuesta = client.get("/donaciones?estado=INVALIDO",
          req -> req.header("Authorization", "Bearer " + token));
      assertEquals(400, respuesta.code());
    });
  }

  // === Helpers ===

  private Javalin app(GestorDonaciones gestor, RepositorioUsuarios repoUsuarios) {
    Javalin app = Javalin.create();
    new FiltroDeAcceso(verificador, repoUsuarios).registrarEn(app);
    new DonacionesController(
        gestor,
        mock(RepositorioPersonas.class),
        mock(RepositorioDonantes.class),
        mock(RepositorioSubcategorias.class)
    ).registrarRutas(app);

    app.exception(DatosInvalidosException.class, (e, ctx) ->
        ctx.status(400).json(new ErrorDTO(e.getMessage())));
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

  private PersonaJuridica personaJuridicaConId(long id) throws Exception {
    PersonaJuridica persona = new PersonaJuridica(
        "30-" + id + "-7", "Escuela " + id, TipoOrganizacion.INSTITUCION, "Educacion");
    setPrivateField(persona.getClass().getSuperclass(), persona, "id", id);
    return persona;
  }

  private Donante donanteConId(long id, PersonaHumana persona) throws Exception {
    Donante donante = new Donante(persona);
    setPrivateField(Donante.class, donante, "id", id);
    return donante;
  }

  private Beneficiaria beneficiariaConId(long id, PersonaJuridica persona) throws Exception {
    Beneficiaria beneficiaria = new Beneficiaria(persona);
    setPrivateField(Beneficiaria.class, beneficiaria, "id", id);
    return beneficiaria;
  }

  private Donacion donacionDe(Donante donante,
                              String nombreSubcategoria,
                              String nombreCategoria,
                              long donacionId) throws Exception {
    Categoria categoria = new Categoria(nombreCategoria);
    Subcategoria subcategoria = new Subcategoria(nombreSubcategoria, categoria, Unidades.UNIDADES);
    Bien bien = new Bien("bien", subcategoria, 1, CondicionBien.USADO);
    Donacion donacion = Donacion.crear(List.of(bien), donante, "desc");
    setPrivateField(Donacion.class, donacion, "id", donacionId);
    return donacion;
  }

  private void setPrivateField(Class<?> clase, Object objeto, String nombre, Object valor)
      throws Exception {
    Field campo = clase.getDeclaredField(nombre);
    campo.setAccessible(true);
    campo.set(objeto, valor);
  }
}
