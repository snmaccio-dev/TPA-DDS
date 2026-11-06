package donatrack.donaciones.infrastructure.identidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import donatrack.donaciones.domain.ConflictoDeNegocioException;
import donatrack.donaciones.seguridad.RolDeAcceso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Usa com.sun.net.httpserver.HttpServer (parte del JDK) para no agregar
// dependencias nuevas. Tampoco hay red externa: todo escucha en 127.0.0.1
// en un puerto libre.
class ProveedorDeIdentidadKeycloakTest {

  private HttpServer servidor;
  private String urlBase;
  private final List<String> rutasLlamadas = new ArrayList<>();
  private final Map<String, Secuencia> respuestas = new ConcurrentHashMap<>();
  private final AtomicInteger contadorTokens = new AtomicInteger(0);
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void arrancarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
    servidor.start();
  }

  @AfterEach
  void frenarServidor() {
    if (servidor != null) {
      servidor.stop(0);
    }
  }

  @Test
  void pideTokenDeServicioYLoReutilizaMientrasNoVence() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("POST", "/admin/realms/donatrack/users", 201, "", "/users/kc-id-1");
    registrarEndpoint("GET", "/admin/realms/donatrack/roles/DONANTE", 200, "{\"name\":\"DONANTE\"}", null);
    registrarEndpoint("POST", "/admin/realms/donatrack/users/kc-id-1/role-mappings/realm", 204, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();

    String id1 = proveedor.crearUsuario("a@mail.com", "pw12345678", RolDeAcceso.DONANTE);

    registrarEndpoint("POST", "/admin/realms/donatrack/users", 201, "", "/users/kc-id-2");
    registrarEndpoint("POST", "/admin/realms/donatrack/users/kc-id-2/role-mappings/realm", 204, "", null);
    String id2 = proveedor.crearUsuario("b@mail.com", "pw12345678", RolDeAcceso.DONANTE);

    assertEquals("kc-id-1", id1);
    assertEquals("kc-id-2", id2);
    assertEquals(1, contadorTokens.get(),
        "El token tiene que reutilizarse mientras no vence.");
  }

  @Test
  void crearUsuarioDevuelveElIdDelHeaderLocationYAsignaElRol() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("POST", "/admin/realms/donatrack/users", 201, "", "/users/kc-abc");
    registrarEndpoint("GET", "/admin/realms/donatrack/roles/BENEFICIARIA", 200, "{\"name\":\"BENEFICIARIA\"}", null);
    registrarEndpoint("POST", "/admin/realms/donatrack/users/kc-abc/role-mappings/realm", 204, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    String id = proveedor.crearUsuario("x@mail.com", "pw12345678", RolDeAcceso.BENEFICIARIA);

    assertEquals("kc-abc", id);
    assertTrue(rutasLlamadas.contains("POST /admin/realms/donatrack/users"));
    assertTrue(rutasLlamadas.contains("GET /admin/realms/donatrack/roles/BENEFICIARIA"));
    assertTrue(rutasLlamadas.contains("POST /admin/realms/donatrack/users/kc-abc/role-mappings/realm"));
  }

  @Test
  void un409AlCrearUsuarioSeConvierteEnConflictoDeNegocio() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("POST", "/admin/realms/donatrack/users", 409, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    ConflictoDeNegocioException e = assertThrows(ConflictoDeNegocioException.class,
        () -> proveedor.crearUsuario("x@mail.com", "pw12345678", RolDeAcceso.DONANTE));

    assertFalse(e.getMessage().contains("pw12345678"),
        "El mensaje de la excepcion no debe contener la contrasena.");
  }

  @Test
  void siFallaLaAsignacionDelRolSeBorraElUsuarioRecienCreado() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("POST", "/admin/realms/donatrack/users", 201, "", "/users/kc-borrable");
    registrarEndpoint("GET", "/admin/realms/donatrack/roles/DONANTE", 500, "", null);
    registrarEndpoint("DELETE", "/admin/realms/donatrack/users/kc-borrable", 204, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    assertThrows(RuntimeException.class,
        () -> proveedor.crearUsuario("x@mail.com", "pw12345678", RolDeAcceso.DONANTE));

    assertTrue(rutasLlamadas.contains("DELETE /admin/realms/donatrack/users/kc-borrable"),
        "Si no se puede asignar el rol, el usuario huerfano hay que borrarlo.");
  }

  @Test
  void crearSinContrasenaNoMandaCredentialsYPideElMail() {
    registrarTokenEndpoint(3600);
    List<String> cuerposPost = new ArrayList<>();
    servidor.createContext("/admin/realms/donatrack/users", intercambio -> {
      if ("POST".equals(intercambio.getRequestMethod())) {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        cuerposPost.add(cuerpo);
        rutasLlamadas.add("POST " + intercambio.getRequestURI().getPath());
        intercambio.getResponseHeaders().add("Location", urlBase + "/users/kc-sinpw");
        intercambio.sendResponseHeaders(201, -1);
        intercambio.close();
        return;
      }
      intercambio.sendResponseHeaders(405, -1);
      intercambio.close();
    });
    registrarEndpoint("GET", "/admin/realms/donatrack/roles/BENEFICIARIA", 200, "{\"name\":\"BENEFICIARIA\"}", null);
    registrarEndpoint("POST", "/admin/realms/donatrack/users/kc-sinpw/role-mappings/realm", 204, "", null);
    registrarEndpoint("PUT", "/admin/realms/donatrack/users/kc-sinpw/execute-actions-email", 204, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    String id = proveedor.crearUsuarioSinContrasena("x@mail.com", RolDeAcceso.BENEFICIARIA);

    assertEquals("kc-sinpw", id);
    assertEquals(1, cuerposPost.size());
    assertFalse(cuerposPost.get(0).contains("credentials"),
        "Sin contrasena no se tienen que mandar credentials.");
    assertTrue(rutasLlamadas.contains("PUT /admin/realms/donatrack/users/kc-sinpw/execute-actions-email"),
        "Hay que pedirle a Keycloak que mande el mail de UPDATE_PASSWORD.");
  }

  @Test
  void eliminarUsuarioIgnoraUn404() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("DELETE", "/admin/realms/donatrack/users/no-existe", 404, "", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    proveedor.eliminarUsuario("no-existe"); // no tira
  }

  @Test
  void laContrasenaNoApareceEnLosMensajesDeExcepcion() {
    registrarTokenEndpoint(3600);
    registrarEndpoint("POST", "/admin/realms/donatrack/users", 500, "boom", null);

    ProveedorDeIdentidadKeycloak proveedor = nuevoProveedor();
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> proveedor.crearUsuario("x@mail.com", "SuperSecreta123!", RolDeAcceso.DONANTE));

    assertFalse(e.getMessage().contains("SuperSecreta123!"));
  }

  // === Helpers ===

  private ProveedorDeIdentidadKeycloak nuevoProveedor() {
    return new ProveedorDeIdentidadKeycloak(
        urlBase, "donatrack", "donaciones-backend", "secret-test",
        HttpClient.newHttpClient(), json);
  }

  private void registrarTokenEndpoint(long expiresInSegundos) {
    servidor.createContext("/realms/donatrack/protocol/openid-connect/token", intercambio -> {
      contadorTokens.incrementAndGet();
      rutasLlamadas.add("POST /realms/donatrack/protocol/openid-connect/token");
      String cuerpo = "{\"access_token\":\"token-de-test-"
          + contadorTokens.get() + "\",\"expires_in\":" + expiresInSegundos + "}";
      byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
      intercambio.getResponseHeaders().add("Content-Type", "application/json");
      intercambio.sendResponseHeaders(200, bytes.length);
      intercambio.getResponseBody().write(bytes);
      intercambio.close();
    });
  }

  // El HttpServer del JDK admite un solo contexto por path, asi que registrar la misma
  // ruta dos veces no reemplaza al handler anterior. Por eso cada ruta guarda sus
  // respuestas en orden de registro y un contador de llamadas: la llamada N devuelve la
  // respuesta N, y a partir de ahi se repite la ultima. Eso cubre los endpoints que se
  // registran una vez y se invocan varias, como el de roles.
  private void registrarEndpoint(String metodo, String ruta, int status,
                                 String cuerpo, String locationRelativo) {
    Respuesta respuesta = new Respuesta(metodo, status, cuerpo, locationRelativo);
    Secuencia existente = respuestas.get(ruta);

    if (existente != null) {
      existente.agregar(respuesta);
      return;
    }

    Secuencia secuencia = new Secuencia();
    secuencia.agregar(respuesta);
    respuestas.put(ruta, secuencia);

    servidor.createContext(ruta, intercambio -> {
      Respuesta actual = respuestas.get(ruta).siguiente();

      if (actual == null || !actual.metodo().equals(intercambio.getRequestMethod())) {
        intercambio.sendResponseHeaders(405, -1);
        intercambio.close();
        return;
      }
      rutasLlamadas.add(actual.metodo() + " " + intercambio.getRequestURI().getPath());
      if (actual.locationRelativo() != null) {
        intercambio.getResponseHeaders().add("Location", urlBase + actual.locationRelativo());
      }
      byte[] bytes = actual.cuerpo() == null
          ? new byte[0]
          : actual.cuerpo().getBytes(StandardCharsets.UTF_8);
      if (bytes.length > 0) {
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(actual.status(), bytes.length);
        intercambio.getResponseBody().write(bytes);
      } else {
        intercambio.sendResponseHeaders(actual.status(), -1);
      }
      intercambio.close();
    });
  }

  private record Respuesta(String metodo, int status, String cuerpo, String locationRelativo) {
  }

  private static final class Secuencia {

    private final List<Respuesta> respuestas = new CopyOnWriteArrayList<>();
    private final AtomicInteger llamadas = new AtomicInteger(0);

    private void agregar(Respuesta respuesta) {
      respuestas.add(respuesta);
    }

    private Respuesta siguiente() {
      if (respuestas.isEmpty()) {
        return null;
      }

      return respuestas.get(
          Math.min(llamadas.getAndIncrement(), respuestas.size() - 1)
      );
    }
  }

  // Helper de tests: parsea JSON. Actualmente no se usa pero lo dejamos
  // porque los tests que verifiquen el cuerpo del POST /users lo necesitan.
  @SuppressWarnings("unused")
  private JsonNode parsear(String cuerpo) throws IOException {
    return json.readTree(cuerpo);
  }

  // Lee el cuerpo de una request (para asserts mas finos si hacen falta).
  @SuppressWarnings("unused")
  private static String leerCuerpo(HttpExchange intercambio) throws IOException {
    return new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }
}