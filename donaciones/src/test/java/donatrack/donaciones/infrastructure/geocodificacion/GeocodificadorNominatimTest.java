package donatrack.donaciones.infrastructure.geocodificacion;

import com.sun.net.httpserver.HttpServer;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.infrastructure.IntegracionExternaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra un servidor HTTP local que imita la API /search de Nominatim: nunca sale a
// Internet.
class GeocodificadorNominatimTest {

  private static final String USER_AGENT = "DonaTrack-test/1.0";

  private HttpServer servidor;
  private final List<String> consultas = new CopyOnWriteArrayList<>();
  private final List<String> agentes = new CopyOnWriteArrayList<>();
  private final List<Long> momentos = new CopyOnWriteArrayList<>();
  private volatile int estado = 200;
  private volatile String respuesta = "[]";

  @BeforeEach
  void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/search", intercambio -> {
      momentos.add(System.nanoTime());
      consultas.add(URLDecoder.decode(intercambio.getRequestURI().getRawQuery(),
          StandardCharsets.UTF_8));
      agentes.add(intercambio.getRequestHeaders().getFirst("User-Agent"));
      byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);
      intercambio.sendResponseHeaders(estado, cuerpo.length);
      try (OutputStream salida = intercambio.getResponseBody()) {
        salida.write(cuerpo);
      }
    });
    servidor.start();
  }

  @AfterEach
  void bajarServidor() {
    servidor.stop(0);
  }

  @Test
  void devuelveLasCoordenadasDelPrimerResultadoYMandaElUserAgent() {
    respuesta = "[{\"lat\":\"-34.6356\",\"lon\":\"-58.3647\",\"display_name\":\"Brandsen 805\"}]";

    Optional<Coordenadas> coordenadas = geocodificador(Duration.ZERO)
        .geocodificar("Brandsen 805, La Boca, CABA");

    assertEquals(Optional.of(new Coordenadas(-34.6356, -58.3647)), coordenadas);
    assertEquals(List.of(USER_AGENT), agentes);
    assertTrue(consultas.get(0).contains("q=Brandsen 805, La Boca, CABA"));
    assertTrue(consultas.get(0).contains("format=jsonv2"));
    assertTrue(consultas.get(0).contains("limit=1"));
    assertTrue(consultas.get(0).contains("countrycodes=ar"));
  }

  @Test
  void sinResultadosDevuelveVacio() {
    respuesta = "[]";

    assertEquals(Optional.empty(), geocodificador(Duration.ZERO).geocodificar("No existe 123"));
  }

  @Test
  void unErrorDelServicioEsUnaFallaDeIntegracion() {
    estado = 503;

    assertThrows(IntegracionExternaException.class,
        () -> geocodificador(Duration.ZERO).geocodificar("Brandsen 805"));
  }

  @Test
  void entreUnaConsultaYLaSiguienteRespetaElIntervaloMinimo() {
    Duration intervalo = Duration.ofMillis(300);
    GeocodificadorNominatim geocodificador = geocodificador(intervalo);

    geocodificador.geocodificar("Uno 1");
    geocodificador.geocodificar("Dos 2");

    long separacion = momentos.get(1) - momentos.get(0);
    assertTrue(separacion >= intervalo.toNanos(),
        "Las consultas quedaron separadas por " + separacion / 1_000_000 + " ms.");
  }

  private GeocodificadorNominatim geocodificador(Duration intervalo) {
    return new GeocodificadorNominatim(
        "http://127.0.0.1:" + servidor.getAddress().getPort(), USER_AGENT, "ar",
        HttpClient.newHttpClient(), intervalo);
  }
}
